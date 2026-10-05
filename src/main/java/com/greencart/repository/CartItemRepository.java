package com.greencart.repository;
import com.greencart.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface CartItemRepository extends JpaRepository<CartItem,Long> {
  List<CartItem> findByUser(User user);
  Optional<CartItem> findByUserAndProduct(User u, Product p);
  void deleteByUser(User user);
  long countByUser(User user);
}
