package com.greencart.repository;

import com.greencart.entity.DeliveryStaff;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryStaffRepository extends JpaRepository<DeliveryStaff, Long> {
    Optional<DeliveryStaff> findByUser(User user);
    Optional<DeliveryStaff> findByUserId(Long userId);
    List<DeliveryStaff> findAllByOrderByIdAsc();
    List<DeliveryStaff> findByUser_EnabledTrueOrderByIdAsc();
    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);
    Optional<DeliveryStaff> findByVehicleNumberIgnoreCase(String vehicleNumber);
}
