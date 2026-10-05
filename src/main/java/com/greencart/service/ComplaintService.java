package com.greencart.service;

import com.greencart.entity.Complaint;
import com.greencart.entity.User;
import com.greencart.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository repository;

    public List<Complaint> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> mine(User user) {
        return repository.findByUserOrderByCreatedAtDesc(user);
    }

    public Complaint get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
    }

    public Complaint save(Complaint complaint) {
        return repository.save(complaint);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }
}
