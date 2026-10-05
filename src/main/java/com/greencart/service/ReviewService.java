package com.greencart.service;
import com.greencart.entity.*;
import com.greencart.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
@Service @RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository repo;
  public List<Review> all(){ return repo.findAllByOrderByCreatedAtDesc(); }
  public List<Review> mine(User u){ return repo.findByUserOrderByCreatedAtDesc(u); }
  public Optional<Review> byOrder(Order o){ return repo.findByOrder(o); }
  public boolean existsForOrder(Order o){ return repo.existsByOrder(o); }
  public Review save(Review r){ return repo.save(r); }
  public Review get(Long id){ return repo.findById(id).orElseThrow(); }
  public void delete(Long id){ repo.deleteById(id); }
  public long count(){ return repo.count(); }
}
