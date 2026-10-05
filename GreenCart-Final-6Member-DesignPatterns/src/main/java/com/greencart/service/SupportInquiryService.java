package com.greencart.service;

import com.greencart.entity.SupportInquiry;
import com.greencart.entity.User;
import com.greencart.repository.SupportInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportInquiryService {

    private final SupportInquiryRepository repository;

    public List<SupportInquiry> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<SupportInquiry> mine(User user) {
        return repository.findByUserOrderByCreatedAtDesc(user);
    }

    public SupportInquiry get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support inquiry not found"));
    }

    public SupportInquiry save(SupportInquiry inquiry) {
        return repository.save(inquiry);
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }
}
