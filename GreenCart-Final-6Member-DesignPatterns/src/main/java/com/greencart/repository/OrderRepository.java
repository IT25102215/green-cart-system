package com.greencart.repository;

import com.greencart.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
  List<Order> findByUserOrderByCreatedAtDesc(User user);
  List<Order> findAllByOrderByCreatedAtDesc();

  @Query("select o from Order o left join o.user u where " +
          "(:status is null or o.status = :status) and " +
          "(:fromDate is null or o.createdAt >= :fromDate) and " +
          "(:toDateExclusive is null or o.createdAt < :toDateExclusive) and " +
          "(:q is null or (:orderId is not null and o.id = :orderId) " +
          "or lower(coalesce(u.fullName,'')) like lower(concat('%',:q,'%')) " +
          "or lower(coalesce(u.email,'')) like lower(concat('%',:q,'%')) " +
          "or lower(coalesce(o.address,'')) like lower(concat('%',:q,'%')))")
  Page<Order> searchAdmin(@Param("q") String q,
                          @Param("orderId") Long orderId,
                          @Param("status") OrderStatus status,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDateExclusive") LocalDateTime toDateExclusive,
                          Pageable pageable);

  long countByStatus(OrderStatus status);

  @Query("select coalesce(sum(o.total),0) from Order o where o.status <> com.greencart.entity.OrderStatus.CANCELLED")
  BigDecimal totalIncome();

  @Query("select coalesce(sum(o.total),0) from Order o where o.createdAt >= ?1 and o.status <> com.greencart.entity.OrderStatus.CANCELLED")
  BigDecimal incomeSince(LocalDateTime since);
}
