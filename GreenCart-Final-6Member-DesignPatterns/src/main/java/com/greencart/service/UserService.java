package com.greencart.service;

import com.greencart.dto.RegisterDto;
import com.greencart.entity.*;
import com.greencart.repository.UserRepository;
import com.greencart.util.InputValidation;
import com.greencart.util.PhoneValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repo;
  private final PasswordEncoder encoder;

  public User register(RegisterDto d){
    String email = InputValidation.requireEmail(d.getEmail());
    String password = InputValidation.requireStrongPassword(d.getPassword());
    if(repo.existsByEmail(email)) throw new RuntimeException("Email already in use");
    User u = User.builder()
      .fullName(InputValidation.requireText(d.getFullName(), "Full name", 2, 100))
      .email(email)
      .password(encoder.encode(password))
      .phone(PhoneValidation.requireTenDigits(d.getPhone()))
      .address(InputValidation.requireText(d.getAddress(), "Address", 5, 255))
      .role(Role.USER).enabled(true).build();
    return repo.save(u);
  }

  public User getCurrent(Authentication auth){
    if(auth==null) return null;
    return repo.findByEmail(auth.getName()).orElse(null);
  }

  public User byEmail(String e){ return repo.findByEmail(e).orElseThrow(); }
  public List<User> all(){ return repo.findAll(); }
  public User get(Long id){ return repo.findById(id).orElseThrow(); }
  public User save(User u){ return repo.save(u); }
  public long count(){ return repo.count(); }
  public void toggleEnabled(Long id){ User u = get(id); u.setEnabled(!u.isEnabled()); repo.save(u); }

  public boolean changePassword(User u, String oldPw, String newPw){
    if(!encoder.matches(oldPw, u.getPassword())) return false;
    InputValidation.requireStrongPassword(newPw);
    u.setPassword(encoder.encode(newPw)); repo.save(u); return true;
  }

  public String createResetToken(String email){
    String cleanEmail;
    try {
      cleanEmail = InputValidation.requireEmail(email);
    } catch (IllegalArgumentException ex) {
      return null;
    }
    Optional<User> opt = repo.findByEmail(cleanEmail);
    if(opt.isEmpty()) return null;
    User u = opt.get();
    String token = UUID.randomUUID().toString();
    u.setResetToken(token); u.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
    repo.save(u); return token;
  }

  public boolean resetPassword(String token, String newPw){
    Optional<User> opt = repo.findByResetToken(token);
    if(opt.isEmpty()) return false;
    User u = opt.get();
    if(u.getResetTokenExpiry()==null || u.getResetTokenExpiry().isBefore(LocalDateTime.now())) return false;
    InputValidation.requireStrongPassword(newPw);
    u.setPassword(encoder.encode(newPw)); u.setResetToken(null); u.setResetTokenExpiry(null);
    repo.save(u); return true;
  }
}
