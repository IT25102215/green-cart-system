package com.greencart.service;

import com.greencart.entity.DeliveryStaff;
import com.greencart.entity.Role;
import com.greencart.entity.User;
import com.greencart.repository.DeliveryRepository;
import com.greencart.repository.DeliveryStaffRepository;
import com.greencart.repository.UserRepository;
import com.greencart.util.InputValidation;
import com.greencart.util.PhoneValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Staff administration for Member 4 - Delivery Management.
 * Delivery managers create and maintain delivery-person accounts here;
 * delivery personnel themselves only work on deliveries assigned to them.
 */
@Service
@RequiredArgsConstructor
public class DeliveryStaffManagementService {

    private final UserRepository userRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final DeliveryRepository deliveryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public DeliveryStaff create(String fullName,
                                String email,
                                String phone,
                                String rawPassword,
                                String vehicleNumber,
                                int maxActiveOrders) {
        String cleanName = InputValidation.requireText(fullName, "Full name", 2, 100);
        String cleanEmail = InputValidation.requireEmail(email);
        String cleanPhone = PhoneValidation.requireTenDigits(phone);
        String cleanPassword = InputValidation.requireStrongPassword(rawPassword);
        String cleanVehicle = normalizeVehicle(vehicleNumber);
        validateCapacity(maxActiveOrders);

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (deliveryStaffRepository.existsByVehicleNumberIgnoreCase(cleanVehicle)) {
            throw new IllegalArgumentException("Vehicle number is already assigned to another delivery staff member");
        }

        User user = userRepository.save(User.builder()
                .fullName(cleanName)
                .email(cleanEmail)
                .password(passwordEncoder.encode(cleanPassword))
                .phone(cleanPhone)
                .role(Role.DELIVERY_PERSON)
                .enabled(true)
                .build());

        return deliveryStaffRepository.saveAndFlush(DeliveryStaff.builder()
                .user(user)
                .vehicleNumber(cleanVehicle)
                .availabilityStatus("AVAILABLE")
                .maxActiveOrders(maxActiveOrders)
                .build());
    }

    @Transactional
    public DeliveryStaff update(Long staffId,
                                String fullName,
                                String email,
                                String phone,
                                String vehicleNumber) {
        DeliveryStaff staff = get(staffId);
        User user = staff.getUser();
        if (user == null || user.getRole() != Role.DELIVERY_PERSON) {
            throw new IllegalArgumentException("Delivery staff profile is not linked to a delivery-person account");
        }

        String cleanName = InputValidation.requireText(fullName, "Full name", 2, 100);
        String cleanEmail = InputValidation.requireEmail(email);
        String cleanPhone = PhoneValidation.requireTenDigits(phone);
        String cleanVehicle = normalizeVehicle(vehicleNumber);

        userRepository.findByEmail(cleanEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Email is already in use"); });

        deliveryStaffRepository.findByVehicleNumberIgnoreCase(cleanVehicle)
                .filter(existing -> !existing.getId().equals(staff.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Vehicle number is already assigned to another delivery staff member"); });

        user.setFullName(cleanName);
        user.setEmail(cleanEmail);
        user.setPhone(cleanPhone);
        userRepository.save(user);

        staff.setVehicleNumber(cleanVehicle);
        return deliveryStaffRepository.saveAndFlush(staff);
    }

    @Transactional
    public DeliveryStaff setEnabled(Long staffId, boolean enabled) {
        DeliveryStaff staff = get(staffId);
        User user = staff.getUser();
        if (user == null || user.getRole() != Role.DELIVERY_PERSON) {
            throw new IllegalArgumentException("Delivery staff profile is not linked to a delivery-person account");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        staff.setAvailabilityStatus(enabled ? "AVAILABLE" : "DISABLED");
        return deliveryStaffRepository.saveAndFlush(staff);
    }

    public DeliveryStaff get(Long staffId) {
        return deliveryStaffRepository.findById(staffId)
                .orElseThrow(() -> new NoSuchElementException("Delivery staff not found"));
    }

    /**
     * Remove a delivery-person account from the current staff roster.
     * Historical deliveries are preserved; their staff reference becomes null.
     * The controller blocks this action while the staff member still has active work.
     */
    @Transactional
    public String remove(Long staffId) {
        DeliveryStaff staff = get(staffId);
        User user = staff.getUser();
        String name = user == null ? "Delivery staff" : user.getFullName();

        deliveryRepository.findByDeliveryStaffOrderByUpdatedAtDesc(staff).forEach(delivery -> {
            delivery.setDeliveryStaff(null);
            deliveryRepository.save(delivery);
        });
        deliveryRepository.flush();

        deliveryStaffRepository.delete(staff);
        deliveryStaffRepository.flush();

        if (user != null && user.getRole() == Role.DELIVERY_PERSON) {
            userRepository.delete(user);
            userRepository.flush();
        }
        return name;
    }

    private String normalizeVehicle(String value) {
        return InputValidation.requireText(value, "Vehicle number", 3, 30).toUpperCase(Locale.ROOT);
    }

    private void validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 20) {
            throw new IllegalArgumentException("Capacity must be between 1 and 20");
        }
    }
}
