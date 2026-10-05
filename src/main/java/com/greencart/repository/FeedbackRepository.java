package com.greencart.repository;

import com.greencart.entity.Feedback;
import com.greencart.entity.FeedbackStatus;
import com.greencart.entity.Order;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    List<Feedback> findByUserOrderByCreatedAtDesc(User user);
    Optional<Feedback> findByOrder(Order order);
    boolean existsByOrder(Order order);
    long countByStatus(FeedbackStatus status);
}
