package com.greencart.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class GlobalExceptionHandler implements ErrorController {
  @RequestMapping("/error")
  public String error(HttpServletRequest req, Model model){
    Object status = req.getAttribute("jakarta.servlet.error.status_code");
    int code = status==null?500:Integer.parseInt(status.toString());
    model.addAttribute("status", code);
    model.addAttribute("message", HttpStatus.valueOf(code).getReasonPhrase());
    if(code == 404) return "error/404";
    return "error/500";
  }
}
