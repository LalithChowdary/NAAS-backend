package com.naas.backend.customer;

import com.naas.backend.auth.entity.User;
import com.naas.backend.customer.dto.CreateCustomerAddressRequest;
import com.naas.backend.customer.dto.CustomerAddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerRepository customerRepository;

    private CustomerAddressResponse toResponse(CustomerAddress address) {
        return CustomerAddressResponse.builder()
                .id(address.getId())
                .customerId(address.getCustomer().getId())
                .label(address.getLabel())
                .address(address.getAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .house(address.getHouse())
                .area(address.getArea())
                .landmark(address.getLandmark())
                .build();
    }

    public List<CustomerAddressResponse> getCustomerAddresses(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        return customerAddressRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(customer.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerAddressResponse addCustomerAddress(User user, CreateCustomerAddressRequest request) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        CustomerAddress address = CustomerAddress.builder()
                .customer(customer)
                .label(request.getLabel())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .house(request.getHouse())
                .area(request.getArea())
                .landmark(request.getLandmark())
                .active(true)
                .build();

        customerAddressRepository.save(address);
        return toResponse(address);
    }
}
