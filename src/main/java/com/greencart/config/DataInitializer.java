package com.greencart.config;

import com.greencart.entity.*;
import com.greencart.repository.DeliveryRepository;
import com.greencart.repository.DeliveryStaffRepository;
import com.greencart.repository.OrderRepository;
import com.greencart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepo;
    private final DeliveryStaffRepository deliveryStaffRepo;
    private final DeliveryRepository deliveryRepo;
    private final OrderRepository orderRepo;
    private final PasswordEncoder encoder;

    private static final Set<String> LEGACY_DELIVERY_STAFF_EMAILS = Set.of(
            "delivery@greencart.com",
            "delivery2@greencart.com",
            "delivery3@greencart.com",
            "delivery4@greencart.com",
            "delivery5@greencart.com"
    );

    @Override
    public void run(String... args) {
        ensureUser("Admin", "admin@greencart.com", "Admin@123", "0710000000", Role.ADMIN);
        ensureUser("Business Owner", "owner@greencart.com", "Owner@123", "0711000000", Role.BUSINESS_OWNER);
        ensureUser("Product Manager", "product@greencart.com", "Product@123", "0712000000", Role.PRODUCT_MANAGER);
        ensureUser("Order Administrator", "orders@greencart.com", "Orders@123", "0713000000", Role.ORDER_ADMIN);
        ensureUser("Supplier Manager", "supplier@greencart.com", "Supplier@123", "0714000000", Role.SUPPLIER_MANAGER);
        ensureUser("Feedback Administrator", "feedback@greencart.com", "Feedback@123", "0715000000", Role.FEEDBACK_ADMIN);
        ensureUser("Delivery Manager", "deliverymanager@greencart.com", "DeliveryManager@123", "0716000000", Role.DELIVERY_MANAGER);

        // Delivery personnel are NOT seeded anymore. They must be created from
        // Delivery Management -> Add Delivery Staff by the Delivery Manager/Admin.
        removeLegacySeededDeliveryStaff();


        // Existing databases may receive the new capacity column through Hibernate schema update.
        // Normalize legacy/custom staff rows once without overwriting valid administrator choices.
        deliveryStaffRepo.findAll().forEach(staff -> {
            if (staff.getMaxActiveOrders() == null || staff.getMaxActiveOrders() < 1) {
                staff.setMaxActiveOrders(5);
                deliveryStaffRepo.save(staff);
            }
        });

        User demoCustomer = ensureUser("Demo Customer", "user@greencart.com", "User@123", "0750000000", Role.USER);
        if (demoCustomer.getAddress() == null || demoCustomer.getAddress().isBlank()) {
            demoCustomer.setAddress("Matara, Sri Lanka");
            userRepo.save(demoCustomer);
        }
    }

    private User ensureUser(String fullName, String email, String rawPassword, String phone, Role role) {
        User user = userRepo.findByEmail(email).orElseGet(() -> User.builder()
                .fullName(fullName)
                .email(email)
                .password(encoder.encode(rawPassword))
                .phone(phone)
                .role(role)
                .enabled(true)
                .build());

        boolean changed = user.getId() == null;
        if (!fullName.equals(user.getFullName())) { user.setFullName(fullName); changed = true; }
        if (!phone.equals(user.getPhone())) { user.setPhone(phone); changed = true; }
        if (user.getRole() != role) { user.setRole(role); changed = true; }
        if (!user.isEnabled()) { user.setEnabled(true); changed = true; }
        // Existing users keep passwords they have changed. A default password is only
        // applied when the account is first created (or if a legacy row has no password).
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(encoder.encode(rawPassword));
            changed = true;
        }
        return changed ? userRepo.save(user) : user;
    }

    /**
     * Removes the five old demo delivery-person accounts from earlier builds.
     * Any delivery that still references one of those demo users is detached first.
     * Active legacy deliveries are moved back to FAILED/CONFIRMED so a newly-created
     * staff member can be assigned safely from the Delivery Manager console.
     */
    private void removeLegacySeededDeliveryStaff() {
        for (String email : LEGACY_DELIVERY_STAFF_EMAILS) {
            userRepo.findByEmail(email).ifPresent(user -> {
                deliveryStaffRepo.findByUser(user).ifPresent(staff -> {
                    deliveryRepo.findByDeliveryStaffOrderByUpdatedAtDesc(staff).forEach(delivery -> {
                        delivery.setDeliveryStaff(null);

                        if (delivery.getStatus() != DeliveryStatus.DELIVERED
                                && delivery.getStatus() != DeliveryStatus.FAILED) {
                            delivery.setStatus(DeliveryStatus.FAILED);
                            delivery.setProblem("Previous demo delivery staff removed. Reassign to a new delivery staff member.");

                            Order order = delivery.getOrder();
                            if (order != null
                                    && order.getStatus() != OrderStatus.DELIVERED
                                    && order.getStatus() != OrderStatus.CANCELLED) {
                                order.setStatus(OrderStatus.CONFIRMED);
                                orderRepo.save(order);
                            }
                        }

                        deliveryRepo.save(delivery);
                    });
                    deliveryRepo.flush();
                    deliveryStaffRepo.delete(staff);
                    deliveryStaffRepo.flush();
                });

                // Only remove the known legacy DELIVERY_PERSON demo accounts.
                if (user.getRole() == Role.DELIVERY_PERSON) {
                    userRepo.delete(user);
                    userRepo.flush();
                }
            });
        }
    }

}
