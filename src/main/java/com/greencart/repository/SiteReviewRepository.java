package com.greencart.repository;

import com.greencart.entity.SiteReview;
import com.greencart.entity.SiteReviewStatus;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteReviewRepository extends JpaRepository<SiteReview, Long> {
    List<SiteReview> findAllByOrderByCreatedAtDesc();
    List<SiteReview> findByStatusOrderByCreatedAtDesc(SiteReviewStatus status);
    Optional<SiteReview> findByUser(User user);
    long countByStatus(SiteReviewStatus status);
}
