package com.greencart.feature.feedback;

import com.greencart.designpattern.member6support.observer.SupportRequestEvent;
import com.greencart.designpattern.member6support.observer.SupportRequestSubject;
import com.greencart.entity.SiteReview;
import com.greencart.entity.User;
import com.greencart.service.AuditService;
import com.greencart.service.SiteReviewService;
import com.greencart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SiteReviewController {
    private final SiteReviewService siteReviews;
    private final UserService users;
    private final AuditService auditService;
    private final SupportRequestSubject supportRequestSubject;

    @GetMapping("/site-reviews")
    public String customerPage(Model model, Authentication authentication) {
        User me = users.getCurrent(authentication);
        model.addAttribute("mySiteReview", siteReviews.mine(me).orElse(null));
        model.addAttribute("approvedSiteReviews", siteReviews.approved());
        return "user/site-reviews";
    }

    @PostMapping("/site-reviews")
    public String submit(@RequestParam Integer rating,
                         @RequestParam String comment,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            User me = users.getCurrent(authentication);
            SiteReview saved = siteReviews.submitOrUpdate(me, rating, comment);
            supportRequestSubject.notifyObservers(new SupportRequestEvent(
                    "SITE_REVIEW",
                    saved.getId(),
                    me == null ? null : me.getFullName(),
                    me == null ? null : me.getEmail(),
                    "Green Cart website review - " + rating + " star(s)",
                    saved.getComment()
            ));
            redirectAttributes.addFlashAttribute("success",
                    "Thank you. Your Green Cart review was submitted and is waiting for admin approval.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not submit review: " + ex.getMessage());
        }
        return "redirect:/site-reviews";
    }

    @PostMapping("/site-reviews/delete")
    public String deleteOwn(Authentication authentication, RedirectAttributes redirectAttributes) {
        User me = users.getCurrent(authentication);
        siteReviews.deleteOwn(me);
        redirectAttributes.addFlashAttribute("success", "Your Green Cart review was removed.");
        return "redirect:/site-reviews";
    }

    @GetMapping("/admin/site-reviews")
    public String adminPage(Model model) {
        model.addAttribute("siteReviews", siteReviews.all());
        model.addAttribute("pendingSiteReviews", siteReviews.pendingCount());
        return "admin/site-reviews";
    }

    @PostMapping("/admin/site-reviews/{id}/approve")
    public String approve(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        SiteReview review = siteReviews.approve(id, authentication.getName());
        auditService.log("SUPPORT", "APPROVE_SITE_REVIEW", "SiteReview", review.getId(),
                "Approved customer website review", authentication);
        redirectAttributes.addFlashAttribute("success", "Review approved and is now visible to customers.");
        return "redirect:/admin/site-reviews";
    }

    @PostMapping("/admin/site-reviews/{id}/reject")
    public String reject(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        SiteReview review = siteReviews.reject(id, authentication.getName());
        auditService.log("SUPPORT", "REJECT_SITE_REVIEW", "SiteReview", review.getId(),
                "Rejected customer website review", authentication);
        redirectAttributes.addFlashAttribute("success", "Review rejected. It will not be shown publicly.");
        return "redirect:/admin/site-reviews";
    }

    @PostMapping("/admin/site-reviews/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        auditService.log("SUPPORT", "DELETE_SITE_REVIEW", "SiteReview", id,
                "Deleted customer website review", authentication);
        siteReviews.delete(id);
        redirectAttributes.addFlashAttribute("success", "Review deleted.");
        return "redirect:/admin/site-reviews";
    }
}
