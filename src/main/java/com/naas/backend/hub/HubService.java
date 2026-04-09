package com.naas.backend.hub;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;

    private static final double DEFAULT_HUB_LAT = 16.49830891918236;
    private static final double DEFAULT_HUB_LNG = 80.65770319035148;

    @PostConstruct
    public void seedDefaultHub() {
        if (hubRepository.count() == 0) {
            Hub defaultHub = Hub.builder()
                    .name("Main Delivery Hub")
                    .address("Default Application Properties Hub")
                    .latitude(DEFAULT_HUB_LAT)
                    .longitude(DEFAULT_HUB_LNG)
                    .active(true)
                    .build();
            hubRepository.save(defaultHub);
            log.info("Seeded default Main Delivery Hub based on application.properties coordinates.");
        }
    }

    public List<Hub> getAllHubs() {
        return hubRepository.findAll();
    }

    public Hub getHubById(UUID id) {
        return hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found"));
    }

    public Hub createHub(Hub hub) {
        return hubRepository.save(hub);
    }

    public Hub updateHub(UUID id, Hub hubDetails) {
        Hub hub = getHubById(id);
        hub.setName(hubDetails.getName());
        hub.setAddress(hubDetails.getAddress());
        hub.setLatitude(hubDetails.getLatitude());
        hub.setLongitude(hubDetails.getLongitude());
        hub.setActive(hubDetails.isActive());
        return hubRepository.save(hub);
    }

    public void deleteHub(UUID id) {
        Hub hub = getHubById(id);
        hubRepository.delete(hub);
    }
}
