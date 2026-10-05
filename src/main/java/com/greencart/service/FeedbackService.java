package com.greencart.service;

import com.greencart.entity.Feedback;
import com.greencart.entity.Order;
import com.greencart.entity.User;
import com.greencart.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Customer Support & Feedback business logic - IT25101421 (Chathushka A.H.S.). */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository repository;

    public List<Feedback> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> mine(User user) {
        return repository.findByUserOrderByCreatedAtDesc(user);
    }

    public Feedback get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));
    }

    public Feedback save(Feedback feedback) {
        return repository.save(feedback);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public boolean existsForOrder(Order order) {
        return repository.existsByOrder(order);
    }

    public long count() {
        return repository.count();
    }
}
