package com.greencart.repository;

import com.greencart.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findAllByOrderByUpdatedAtDesc();
    List<Delivery> findByDeliveryStaffOrderByUpdatedAtDesc(DeliveryStaff deliveryStaff);
    List<Delivery> findByDeliveryStaff_User_IdOrderByUpdatedAtDesc(Long userId);
    Optional<Delivery> findByOrder(Order order);
    long countByStatus(DeliveryStatus status);
    long countByDeliveryStaffAndStatusIn(DeliveryStaff deliveryStaff, Collection<DeliveryStatus> statuses);

    @Query("select d from Delivery d where " +
            "(:status is null or d.status = :status) and " +
            "(:staffId is null or d.deliveryStaff.id = :staffId) and " +
            "(:q is null or lower(coalesce(d.order.user.fullName,'')) like lower(concat('%',:q,'%')) " +
            "or lower(coalesce(d.order.address,'')) like lower(concat('%',:q,'%')) " +
            "or lower(coalesce(d.deliveryStaff.user.fullName,'')) like lower(concat('%',:q,'%')))")
    Page<Delivery> searchAdmin(@Param("q") String q,
                               @Param("status") DeliveryStatus status,
                               @Param("staffId") Long staffId,
                               Pageable pageable);
}
