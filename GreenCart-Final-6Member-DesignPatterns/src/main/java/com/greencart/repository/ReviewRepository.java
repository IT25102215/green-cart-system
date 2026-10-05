package com.greencart.repository;
import com.greencart.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ReviewRepository extends JpaRepository<Review,Long> {
  List<Review> findAllByOrderByCreatedAtDesc();
  List<Review> findByUserOrderByCreatedAtDesc(User user);
  Optional<Review> findByOrder(Order order);
  boolean existsByOrder(Order order);
}
