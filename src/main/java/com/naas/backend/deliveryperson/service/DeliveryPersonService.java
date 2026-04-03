package com.naas.backend.deliveryperson.service;

import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.publication.PublicationRepository;
import com.naas.backend.publication.Publication;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryPersonService {

    private final DeliveryPersonRepository deliveryPersonRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final PublicationRepository publicationRepository;
    private final com.naas.backend.subscription.SubscriptionRepository subscriptionRepository;

    public DeliveryPerson createDeliveryPerson(String name, String email, String password, String phone,
            String employeeId, String assignedArea) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.DELIVERY_PERSON)
                .active(true)
                .build();
        userRepository.save(user);

        DeliveryPerson person = DeliveryPerson.builder()
                .user(user)
                .name(name)
                .phone(phone)
                .employeeId(employeeId)
                .assignedArea(assignedArea)
                .status("APPROVED")
                .build();
        return deliveryPersonRepository.save(person);
    }

    public DeliveryPerson signupRequest(String name, String email, String password, String phone, String assignedArea) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(User.Role.DELIVERY_PERSON)
                .active(false) // Pending admin approval
                .build();
        userRepository.save(user);

        DeliveryPerson person = DeliveryPerson.builder()
                .user(user)
                .name(name)
                .phone(phone)
                .assignedArea(assignedArea)
                .status("PENDING")
                .build();
        return deliveryPersonRepository.save(person);
    }
    
    public List<DeliveryPerson> getPendingRequests() {
        return deliveryPersonRepository.findByStatus("PENDING");
    }
    
    public DeliveryPerson approveDeliveryPerson(Long id) {
        DeliveryPerson person = deliveryPersonRepository.findById(id).orElseThrow();
        person.setStatus("APPROVED");
        User user = person.getUser();
        user.setActive(true);
        userRepository.save(user);
        return deliveryPersonRepository.save(person);
    }
    
    public DeliveryPerson rejectDeliveryPerson(Long id) {
        DeliveryPerson person = deliveryPersonRepository.findById(id).orElseThrow();
        person.setStatus("REJECTED");
        return deliveryPersonRepository.save(person);
    }

    public List<DeliveryPerson> getAllDeliveryPersonnel() {
        return deliveryPersonRepository.findAll();
    }

    public DeliveryPerson getByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return deliveryPersonRepository.findByUser(user).orElseThrow();
    }

    public DeliveryPerson assignArea(Long id, String area) {
        DeliveryPerson person = deliveryPersonRepository.findById(id).orElseThrow();
        person.setAssignedArea(area);
        return deliveryPersonRepository.save(person);
    }

    public DeliveryPerson updateProfile(String email, String name, String phone, String payoutDetails) {
        DeliveryPerson person = getByEmail(email);
        if (name != null)
            person.setName(name);
        if (phone != null)
            person.setPhone(phone);
        if (payoutDetails != null)
            person.setPayoutDetails(payoutDetails);
        return deliveryPersonRepository.save(person);
    }

    public DeliveryPerson updateDeliveryPersonAsAdmin(Long id, String name, String phone, String employeeId,
            String assignedArea, String payoutDetails) {
        DeliveryPerson person = deliveryPersonRepository.findById(id).orElseThrow();
        if (name != null)
            person.setName(name);
        if (phone != null)
            person.setPhone(phone);
        if (employeeId != null)
            person.setEmployeeId(employeeId);
        if (assignedArea != null)
            person.setAssignedArea(assignedArea);
        if (payoutDetails != null)
            person.setPayoutDetails(payoutDetails);
        return deliveryPersonRepository.save(person);
    }

    public Double calculatePayout(Long deliveryPersonId, LocalDate start, LocalDate end) {
        List<DeliveryRecord> deliveries = deliveryRecordRepository
                .findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                        deliveryPersonId, start, end, DeliveryRecord.DeliveryStatus.DELIVERED);

        double totalValue = 0.0;
        for (DeliveryRecord record : deliveries) {
            com.naas.backend.subscription.Subscription sub = subscriptionRepository.findById(record.getSubscriptionId())
                    .orElse(null);
            if (sub != null && sub.getItems() != null) {
                for (com.naas.backend.subscription.SubscriptionItem item : sub.getItems()) {
                    totalValue += item.getPublication().getPrice().doubleValue();
                }
            }
        }
        return totalValue * 0.025; // 2.5% agency rule
    }

    public DeliveryPerson toggleStatus(Long id, boolean active) {
        DeliveryPerson person = deliveryPersonRepository.findById(id).orElseThrow();
        User user = person.getUser();
        user.setActive(active);
        userRepository.save(user);
        return person;
    }

    public DeliveryPerson getDeliveryPersonById(Long id) {
        return deliveryPersonRepository.findById(id).orElseThrow();
    }

    public List<DeliveryRecord> getDeliveriesForDeliveryPerson(Long id) {
        return deliveryRecordRepository.findAll().stream()
                .filter(record -> record.getDeliveryPersonId().equals(id))
                .toList();
    }
}
