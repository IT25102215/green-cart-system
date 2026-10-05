package com.greencart.controller;
import com.greencart.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpSession;
@Controller @RequiredArgsConstructor
public class ShopController {
  private final ProductService products;
  @GetMapping("/shop") public String shop(@RequestParam(required=false) String q,
                                          @RequestParam(required=false) Long category,
                                          @RequestParam(defaultValue="0") int page, Model m, Authentication a, HttpSession session){
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"))) return "redirect:/admin/dashboard";
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_DELIVERY_PERSON"))) return "redirect:/delivery/dashboard";
    var pg = products.search(q, category, page, 18);
    m.addAttribute("products", pg.getContent());
    m.addAttribute("page", page);
    m.addAttribute("totalPages", pg.getTotalPages());
    m.addAttribute("q", q);
    m.addAttribute("selectedCategory", category);
    return "shop";
  }
  @GetMapping("/offers") public String offers(@RequestParam(defaultValue="0") int page, Model m, Authentication a, HttpSession session){
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"))) return "redirect:/admin/dashboard";
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_DELIVERY_PERSON"))) return "redirect:/delivery/dashboard";
    var pg = products.offers(page, 18);
    m.addAttribute("products", pg.getContent());
    m.addAttribute("page", page);
    m.addAttribute("totalPages", pg.getTotalPages());
    m.addAttribute("q", null);
    m.addAttribute("selectedCategory", null);
    return "shop";
  }
  @GetMapping("/shop/{id}") public String detail(@PathVariable Long id, Model m, Authentication a, HttpSession session){
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"))) return "redirect:/admin/dashboard";
    if (!preview(session) && a != null && a.isAuthenticated() && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_DELIVERY_PERSON"))) return "redirect:/delivery/dashboard";
    m.addAttribute("product", products.get(id));
    return "product-detail";
  }
  private boolean preview(HttpSession session){ return Boolean.TRUE.equals(session.getAttribute("customerSitePreview")); }
}
