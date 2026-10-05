package com.greencart.service;

import com.greencart.entity.ContactMessage;
import com.greencart.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageService {
    private final ContactMessageRepository repo;

    public ContactMessage save(ContactMessage message) {
        return repo.save(message);
    }

    public ContactMessage get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact message not found"));
    }

    public List<ContactMessage> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
}
