package com.greencart.repository;

import com.greencart.entity.SupportInquiry;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportInquiryRepository extends JpaRepository<SupportInquiry, Long> {
    List<SupportInquiry> findAllByOrderByCreatedAtDesc();
    List<SupportInquiry> findByUserOrderByCreatedAtDesc(User user);
}
