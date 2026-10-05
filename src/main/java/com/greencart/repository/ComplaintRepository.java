package com.greencart.repository;

import com.greencart.entity.Complaint;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByUserOrderByCreatedAtDesc(User user);
}
