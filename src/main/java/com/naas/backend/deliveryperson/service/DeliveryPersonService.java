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
                .build();
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

    public Double calculatePayout(Long deliveryPersonId, LocalDate start, LocalDate end) {
        List<DeliveryRecord> deliveries = deliveryRecordRepository
                .findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                        deliveryPersonId, start, end, DeliveryRecord.DeliveryStatus.DELIVERED);

        double totalValue = 0.0;
        for (DeliveryRecord record : deliveries) {
            Publication pub = publicationRepository.findById(record.getPublicationId()).orElse(null);
            if (pub != null) {
                totalValue += pub.getPrice().doubleValue();
            }
        }
        return totalValue * 0.025; // 2.5% agency rule
    }
}
