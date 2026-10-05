package com.greencart.repository;
import com.greencart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User,Long> {
  Optional<User> findByEmail(String email);
  Optional<User> findByResetToken(String token);
  boolean existsByEmail(String email);
  long countByEnabled(boolean enabled);
 java.util.List<User> findByRole(com.greencart.entity.Role role);
}
