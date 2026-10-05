package com.greencart.config;
import com.greencart.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ModelAttribute;
@ControllerAdvice @RequiredArgsConstructor
public class GlobalModelAttributes {
  private final UserService userService;
  private final CartService cartService;
  private final CategoryService categoryService;
  @ModelAttribute public void inject(org.springframework.ui.Model model, Authentication auth, HttpServletRequest request){
    var u = userService.getCurrent(auth);
    model.addAttribute("currentUser", u);
    model.addAttribute("currentPath", request.getRequestURI());
    model.addAttribute("cartCount", u==null?0:cartService.count(u));
    model.addAttribute("cartTotal", u==null?java.math.BigDecimal.ZERO:cartService.total(u));
    model.addAttribute("cartShipping", u==null?java.math.BigDecimal.ZERO:cartService.shippingFee(u));
    model.addAttribute("allCategories", categoryService.all());
    model.addAttribute("customerSitePreview", Boolean.TRUE.equals(request.getSession().getAttribute("customerSitePreview")));
  }
}
