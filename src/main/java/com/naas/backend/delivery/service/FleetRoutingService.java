package com.naas.backend.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.naas.backend.hub.Hub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Calls the Google Cloud Fleet Routing (Route Optimization) API using a
 * Service Account JSON key.
 * <p>
 * API reference:
 * https://cloud.google.com/optimization/docs/reference/rest/v1/projects/optimizeTours
 */
@Slf4j
@Service
public class FleetRoutingService {

    private static final String FLEET_ROUTING_URL =
            "https://routeoptimization.googleapis.com/v1/projects/{projectId}:optimizeTours";
    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private static final double FALLBACK_LAT = 16.49830891918236;
    private static final double FALLBACK_LNG = 80.65770319035148;

    @Value("${google.fleet.service-account-key}")
    private Resource serviceAccountKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RouteAssignment {
        private String subscriptionId;
        private int vehicleIndex;
        private int routeSequence;
        private UUID assignedHubId;
    }

    /**
     * Calls Fleet Routing with all active subscriptions and available drivers.
     *
     * @param stops   List of stops: each entry has "subscriptionId", "lat", "lng"
     * @param drivers Number of drivers (vehicles) available
     * @param hubs    List of physical active Hubs
     * @return List of RouteAssignment
     */
    public List<RouteAssignment> optimizeRoutes(List<Map<String, Object>> stops, int drivers, List<Hub> hubs)
            throws IOException, InterruptedException {

        if (stops.isEmpty() || drivers == 0) {
            return Collections.emptyList();
        }

        String projectId = resolveProjectId();
        String accessToken = getAccessToken();

        String requestBody = buildRequestBody(stops, drivers, hubs);
        log.info("Fleet Routing request:\n{}", requestBody);

        String url = FLEET_ROUTING_URL.replace("{projectId}", projectId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Fleet Routing response [{}]:\n{}", response.statusCode(), response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Fleet Routing API error " + response.statusCode()
                    + ": " + response.body());
        }

        return parseAssignments(response.body(), stops, hubs);
    }

    // -------------------------------------------------------------------
    // Request builder
    // -------------------------------------------------------------------

    private String buildRequestBody(List<Map<String, Object>> stops, int drivers, List<Hub> hubs)
            throws IOException {

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode model = root.putObject("model");

        // Use fallback if no hubs provided
        double defaultLat = hubs.isEmpty() ? FALLBACK_LAT : hubs.get(0).getLatitude();
        double defaultLng = hubs.isEmpty() ? FALLBACK_LNG : hubs.get(0).getLongitude();

        // ----- Vehicles (No forced startLocation) -----
        ArrayNode vehicles = model.putArray("vehicles");
        for (int i = 0; i < drivers; i++) {
            ObjectNode v = vehicles.addObject();
            // We omit startLocation so Google automatically starts the route
            // at the most mathematically optimal location!
        }

        // ----- Shipments (deliveries to customer) -----
        ArrayNode shipments = model.putArray("shipments");
        for (Map<String, Object> stop : stops) {
            double lat = (double) stop.get("lat");
            double lng = (double) stop.get("lng");

            ObjectNode shipment = shipments.addObject();

            // Delivery at customer location (30-second service time)
            ArrayNode deliveries = shipment.putArray("deliveries");
            ObjectNode delivery = deliveries.addObject();
            delivery.set("arrivalLocation", latLng(lat, lng));
            delivery.put("duration", "30s");

            // Very high penalty so Google never skips this stop
            shipment.put("penaltyCost", 1_000_000.0);
        }

        // ----- Small cost per hour to encourage shorter routes -----
        model.put("globalDurationCostPerHour", 1.0);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private ObjectNode latLng(double lat, double lng) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("latitude", lat);
        node.put("longitude", lng);
        return node;
    }

    // -------------------------------------------------------------------
    // Response parser
    // -------------------------------------------------------------------

    /**
     * Walks the "routes" array in the optimizeTours response and maps each
     * shipment sequence back to its subscriptionId, calculating nearest Hub.
     */
    private List<RouteAssignment> parseAssignments(String responseBody,
                                                   List<Map<String, Object>> stops,
                                                   List<Hub> hubs)
            throws IOException {

        List<RouteAssignment> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode routes = root.path("routes");

        if (routes.isMissingNode() || !routes.isArray()) {
            log.warn("No routes in Fleet Routing response – falling back to round-robin");
            return result;
        }

        // Parse routed visits
        for (int vehicleIdx = 0; vehicleIdx < routes.size(); vehicleIdx++) {
            JsonNode route = routes.get(vehicleIdx);
            // vehicleIndex is omitted for vehicle 0; default to 0
            int actualVehicleIdx = route.path("vehicleIndex").isMissingNode()
                    ? 0 : route.path("vehicleIndex").asInt();

            JsonNode visits = route.path("visits");
            if (visits.isMissingNode()) continue;

            int sequence = 1;

            UUID calculatedHubId = null;
            
            for (JsonNode visit : visits) {
                // Protobuf drops 0 values, so a missing shipmentIndex means 0
                int shipmentIndex = visit.path("shipmentIndex").asInt(0);
                if (shipmentIndex < 0 || shipmentIndex >= stops.size()) continue;

                Map<String, Object> stop = stops.get(shipmentIndex);
                String subId = (String) stop.get("subscriptionId");

                // Calculate the Hub on the very first delivery of the route!
                if (calculatedHubId == null) {
                   calculatedHubId = findClosestHubId((double)stop.get("lat"), (double)stop.get("lng"), hubs);
                }

                result.add(new RouteAssignment(subId, actualVehicleIdx, sequence++, calculatedHubId));
            }
        }

        // Log any shipments Google still skipped (shouldn't happen with penaltyCost)
        JsonNode skipped = root.path("skippedShipments");
        if (!skipped.isMissingNode() && skipped.isArray() && skipped.size() > 0) {
            log.warn("Fleet Routing skipped {} shipments (will round-robin): {}", skipped.size(), skipped);
        }

        log.info("Fleet Routing parsed {} assignments out of {} stops", result.size(), stops.size());
        return result;
    }

    private UUID findClosestHubId(double lat, double lng, List<Hub> hubs) {
        if (hubs == null || hubs.isEmpty()) return null;
        Hub closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Hub hub : hubs) {
            double dist = haversineDistance(lat, lng, hub.getLatitude(), hub.getLongitude());
            if (dist < minDistance) {
                minDistance = dist;
                closest = hub;
            }
        }
        return closest != null ? closest.getId() : null;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rad of earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // -------------------------------------------------------------------
    // Auth helpers
    // -------------------------------------------------------------------

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(serviceAccountKey.getInputStream())
                .createScoped(SCOPE);
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private String resolveProjectId() throws IOException {
        JsonNode json = objectMapper.readTree(serviceAccountKey.getInputStream());
        return json.path("project_id").asText("swe-naas");
    }
}
