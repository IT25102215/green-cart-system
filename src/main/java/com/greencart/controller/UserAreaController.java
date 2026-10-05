package com.greencart.controller;

import com.greencart.entity.Order;
import com.greencart.entity.OrderStatus;
import com.greencart.entity.Review;
import com.greencart.entity.User;
import com.greencart.service.FileStorageService;
import com.greencart.service.OrderService;
import com.greencart.service.ReviewService;
import com.greencart.service.UserService;
import com.greencart.util.PhoneValidation;
import com.greencart.util.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Shared customer profile and review functions. */
@Controller
@RequiredArgsConstructor
public class UserAreaController {

    private final UserService users;
    private final OrderService orders;
    private final ReviewService reviews;
    private final FileStorageService storage;

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        model.addAttribute("user", users.getCurrent(authentication));
        return "user/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam String fullName,
                              @RequestParam String phone,
                              @RequestParam String address,
                              @RequestParam(required = false) MultipartFile profileImage,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = users.getCurrent(authentication);
            String validatedPhone = PhoneValidation.requireTenDigits(phone);
            user.setFullName(InputValidation.requireText(fullName, "Full name", 2, 100));
            user.setPhone(validatedPhone);
            user.setAddress(InputValidation.requireText(address, "Address", 5, 255));
            if (profileImage != null && !profileImage.isEmpty()) {
                user.setProfileImage(storage.store(profileImage));
            }
            users.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile updated");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update profile: " + ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            boolean changed = users.changePassword(users.getCurrent(authentication), oldPassword, newPassword);
            if (changed) {
                redirectAttributes.addFlashAttribute("success", "Password changed");
            } else {
                redirectAttributes.addFlashAttribute("error", "Old password incorrect");
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/reviews")
    public String myReviews(Model model, Authentication authentication) {
        User me = users.getCurrent(authentication);
        var myOrders = orders.mine(me);
        var availableOrders = myOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> !reviews.existsForOrder(order))
                .toList();
        model.addAttribute("reviews", reviews.mine(me));
        model.addAttribute("availableOrders", availableOrders);
        return "user/reviews";
    }

    @PostMapping("/reviews")
    public String addReview(@RequestParam Long orderId,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            User me = users.getCurrent(authentication);
            Order order = orders.getForUser(orderId, me);
            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new IllegalArgumentException("You can review an order only after it has been delivered.");
            }
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            if (comment == null || comment.isBlank()) {
                throw new IllegalArgumentException("Review comment is required");
            }
            if (reviews.existsForOrder(order)) {
                throw new IllegalArgumentException("You have already reviewed this order");
            }
            reviews.save(Review.builder()
                    .user(me)
                    .order(order)
                    .rating(rating)
                    .comment(comment.trim())
                    .build());
            redirectAttributes.addFlashAttribute("success", "Thank you! Your review was posted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, Authentication authentication) {
        Review review = reviews.get(id);
        User me = users.getCurrent(authentication);
        if (review.getUser() != null && review.getUser().getId().equals(me.getId())) {
            reviews.delete(id);
        }
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/{id}/edit")
    public String editReview(@PathVariable Long id,
                             @RequestParam int rating,
                             @RequestParam String comment,
                             Authentication authentication) {
        Review review = reviews.get(id);
        User me = users.getCurrent(authentication);
        if (review.getUser() != null
                && review.getUser().getId().equals(me.getId())
                && rating >= 1
                && rating <= 5
                && comment != null
                && !comment.isBlank()) {
            review.setRating(rating);
            review.setComment(comment.trim());
            reviews.save(review);
        }
        return "redirect:/reviews";
    }
}
