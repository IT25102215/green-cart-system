package com.greencart.controller;

import com.greencart.entity.ContactMessage;
import com.greencart.entity.ContactMessageStatus;
import com.greencart.service.ContactMessageService;
import com.greencart.service.MailService;
import com.greencart.service.ProductService;
import com.greencart.service.ReviewService;
import com.greencart.service.SiteReviewService;
import com.greencart.util.InputValidation;
import com.greencart.util.PhoneValidation;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class HomeController {
  private static final Set<String> MANAGEMENT_AUTHORITIES = Set.of(
      "ROLE_ADMIN", "ROLE_BUSINESS_OWNER", "ROLE_PRODUCT_MANAGER", "ROLE_ORDER_ADMIN",
      "ROLE_SUPPLIER_MANAGER", "ROLE_FEEDBACK_ADMIN"
  );

  private final ProductService products;
  private final ReviewService reviews;
  private final SiteReviewService siteReviews;
  private final ContactMessageService contactMessageService;
  private final MailService mailService;

  @GetMapping({"/","/home"})
  public String home(Model m, Authentication a, HttpSession session){
    if (!preview(session) && a != null && a.isAuthenticated()) {
      if (hasRole(a, "ROLE_DELIVERY_MANAGER")) return "redirect:/admin/deliveries";
      if (isManagement(a)) return "redirect:/admin/dashboard";
      if (hasRole(a, "ROLE_DELIVERY_PERSON")) return "redirect:/delivery/dashboard";
    }
    List<com.greencart.entity.Product> discounted = products.offers(0, 6).getContent();
    m.addAttribute("featured", discounted.isEmpty() ? products.featured().stream().limit(6).toList() : discounted);
    m.addAttribute("reviews", reviews.all().stream().limit(6).toList());
    m.addAttribute("siteReviews", siteReviews.approved().stream().limit(6).toList());
    return "home";
  }

  @GetMapping("/about")
  public String about(Authentication a, HttpSession session){
    if (!preview(session) && isStaff(a)) return "redirect:/dashboard";
    return "about";
  }

  @GetMapping("/contact")
  public String contact(Authentication a, HttpSession session){
    if (!preview(session) && isStaff(a)) return "redirect:/dashboard";
    return "contact";
  }

  @PostMapping("/contact")
  public String submitContact(@RequestParam String name,
                              @RequestParam String email,
                              @RequestParam String phone,
                              @RequestParam String subject,
                              @RequestParam String message,
                              RedirectAttributes redirectAttributes) {
    try {
      ContactMessage saved = contactMessageService.save(ContactMessage.builder()
          .name(InputValidation.requireText(name, "Name", 2, 100))
          .email(InputValidation.requireEmail(email))
          .phone(PhoneValidation.requireTenDigits(phone))
          .subject(InputValidation.requireText(subject, "Subject", 3, 150))
          .message(InputValidation.requireText(message, "Message", 5, 2000))
          .status(ContactMessageStatus.NEW)
          .build());

      try {
        mailService.notifySupport(saved.getName(), saved.getEmail(), saved.getSubject(), saved.getMessage());
      } catch (Exception ignored) {
        // The database record is the source of truth. SMTP failure must not lose the inquiry.
      }

      redirectAttributes.addFlashAttribute("success", "Your message has been sent to Green Cart support.");
    } catch (Exception ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/contact";
  }

  @GetMapping("/my-account")
  public String myAccount(Authentication a){
    if (a == null || !a.isAuthenticated()) return "redirect:/login";
    if (hasRole(a, "ROLE_DELIVERY_MANAGER")) return "redirect:/admin/deliveries";
    if (isManagement(a)) return "redirect:/admin/dashboard";
    if (hasRole(a, "ROLE_DELIVERY_PERSON")) return "redirect:/delivery/dashboard";
    return "user/my-account";
  }

  @GetMapping("/categories")
  public String categories(Model m, Authentication a, HttpSession session){
    if (!preview(session) && isStaff(a)) return "redirect:/dashboard";
    java.util.Map<com.greencart.entity.Category, java.util.List<com.greencart.entity.Product>> productsByCategory = new java.util.LinkedHashMap<>();
    for (com.greencart.entity.Category c : (java.util.List<com.greencart.entity.Category>) m.getAttribute("allCategories")) {
      productsByCategory.put(c, products.byCategory(c.getId()));
    }
    m.addAttribute("categoryGroups", productsByCategory);
    return "categories";
  }

  @GetMapping("/track-order")
  public String trackOrder(Authentication a, HttpSession session){
    if (a == null || !a.isAuthenticated()) return "redirect:/login";
    if (!preview(session) && isStaff(a)) return "redirect:/dashboard";
    return "redirect:/orders";
  }

  @GetMapping("/store-locator")
  public String storeLocator(Authentication a, HttpSession session){
    if (!preview(session) && isStaff(a)) return "redirect:/dashboard";
    return "store-locator";
  }

  @GetMapping("/dashboard")
  public String dashboard(Authentication a){
    if (a == null || !a.isAuthenticated()) return "redirect:/login";
    if (hasRole(a, "ROLE_DELIVERY_MANAGER")) return "redirect:/admin/deliveries";
    if (isManagement(a)) return "redirect:/admin/dashboard";
    if (hasRole(a, "ROLE_DELIVERY_PERSON")) return "redirect:/delivery/dashboard";
    return "redirect:/profile";
  }

  private boolean preview(HttpSession session){ return Boolean.TRUE.equals(session.getAttribute("customerSitePreview")); }

  private boolean isStaff(Authentication a){
    return isManagement(a) || hasRole(a, "ROLE_DELIVERY_MANAGER") || hasRole(a, "ROLE_DELIVERY_PERSON");
  }

  private boolean isManagement(Authentication a) {
    if (a == null || !a.isAuthenticated()) return false;
    return a.getAuthorities().stream().anyMatch(x -> MANAGEMENT_AUTHORITIES.contains(x.getAuthority()));
  }

  private boolean hasRole(Authentication a, String authority) {
    return a != null && a.isAuthenticated()
        && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals(authority));
  }
}
