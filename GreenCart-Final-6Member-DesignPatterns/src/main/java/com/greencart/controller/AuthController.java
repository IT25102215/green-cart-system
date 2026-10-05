package com.greencart.controller;

import com.greencart.dto.RegisterDto;
import com.greencart.service.MailService;
import com.greencart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
  private final UserService users;
  private final MailService mailService;

  @GetMapping("/login") public String login(){ return "auth/login"; }

  @GetMapping("/register")
  public String registerForm(Model m){
    m.addAttribute("registerDto", new RegisterDto());
    return "auth/register";
  }

  @PostMapping("/register")
  public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                         BindingResult br,
                         RedirectAttributes ra){
    if(br.hasErrors()) return "auth/register";
    try {
      users.register(dto);
    } catch(Exception e){
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/register";
    }
    ra.addFlashAttribute("success","Account created! Please log in.");
    return "redirect:/login";
  }

  @GetMapping("/forgot-password")
  public String forgot(){ return "auth/forgot-password"; }

  @PostMapping("/forgot-password")
  public String forgotSubmit(@RequestParam String email, RedirectAttributes ra){
    String token = users.createResetToken(email);
    if (token != null) {
      try {
        mailService.sendPasswordReset(email, token);
      } catch (Exception ignored) {
        // Do not disclose mail-server or account details in the browser.
      }
    }
    ra.addFlashAttribute("success",
            "If an account exists for that email, a password reset link has been sent. The link expires in 1 hour.");
    return "redirect:/forgot-password";
  }

  @GetMapping("/reset-password")
  public String reset(@RequestParam String token, Model m){
    m.addAttribute("token", token);
    return "auth/reset-password";
  }

  @PostMapping("/reset-password")
  public String resetSubmit(@RequestParam String token,
                            @RequestParam String password,
                            RedirectAttributes ra){
    try {
      if(!users.resetPassword(token,password)){
        ra.addFlashAttribute("error","Invalid or expired token");
        return "redirect:/reset-password?token="+token;
      }
      ra.addFlashAttribute("success","Password updated. Please log in.");
      return "redirect:/login";
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/reset-password?token="+token;
    }
  }
}
