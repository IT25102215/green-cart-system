package com.greencart.feature.feedback;

import com.greencart.designpattern.member6support.observer.SupportRequestEvent;
import com.greencart.designpattern.member6support.observer.SupportRequestSubject;
import com.greencart.entity.*;
import com.greencart.service.*;
import com.greencart.util.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VIVA OWNER: IT25101421 - Chathushka A.H.S.
 * MODULE: Customer Support & Feedback Management
 *
 * Covers:
 * - Customer feedback CRUD
 * - Support inquiries
 * - Complaints
 * - Administrator responses and status management
 */
@Controller
@RequiredArgsConstructor
public class FeedbackManagementController {

    private final FeedbackService feedbackService;
    private final ComplaintService complaintService;
    private final SupportInquiryService supportInquiryService;
    private final OrderService orderService;
    private final UserService userService;
    private final ContactMessageService contactMessageService;
    private final MailService mailService;
    private final AuditService auditService;
    private final SupportRequestSubject supportRequestSubject;

    @GetMapping("/admin/feedback")
    public String adminFeedback(Model model) {
        model.addAttribute("feedbacks", feedbackService.all());
        model.addAttribute("complaints", complaintService.all());
        model.addAttribute("inquiries", supportInquiryService.all());
        model.addAttribute("contactMessages", contactMessageService.all());
        return "admin/feedback";
    }

    @PostMapping("/admin/feedback/{id}/respond")
    public String respond(@PathVariable Long id,
                          @RequestParam String response,
                          @RequestParam(defaultValue = "IN_REVIEW") FeedbackStatus status,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("Response is required");
            }
            Feedback feedback = feedbackService.get(id);
            feedback.setResponse(response.trim());
            feedback.setStatus(status);
            feedbackService.save(feedback);
            auditService.log("SUPPORT", "RESPOND_FEEDBACK", "Feedback", id,
                    "Feedback status set to " + status, authentication);
            redirectAttributes.addFlashAttribute("success", "Feedback response saved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not save response: " + safeMessage(ex));
        }
        return "redirect:/admin/feedback";
    }

    @PostMapping("/admin/complaints/{id}/respond")
    public String respondComplaint(@PathVariable Long id,
                                   @RequestParam(required = false) String response,
                                   @RequestParam ComplaintStatus status,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Complaint complaint = complaintService.get(id);
            complaint.setResponse(response == null || response.isBlank() ? null : response.trim());
            complaint.setStatus(status);
            if (complaint.getAssignedAt() == null && status != ComplaintStatus.OPEN) {
                complaint.setAssignedAt(LocalDateTime.now());
            }
            complaintService.save(complaint);
            auditService.log("SUPPORT", "UPDATE_COMPLAINT", "Complaint", id,
                    "Complaint status set to " + status, authentication);
            redirectAttributes.addFlashAttribute("success", "Complaint updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update complaint: " + safeMessage(ex));
        }
        return "redirect:/admin/feedback";
    }

    @PostMapping("/admin/inquiries/{id}/respond")
    public String respondInquiry(@PathVariable Long id,
                                 @RequestParam(required = false) String responseDetails,
                                 @RequestParam InquiryStatus status,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            SupportInquiry inquiry = supportInquiryService.get(id);
            inquiry.setResponseDetails(
                    responseDetails == null || responseDetails.isBlank() ? null : responseDetails.trim()
            );
            inquiry.setStatus(status);
            supportInquiryService.save(inquiry);
            auditService.log("SUPPORT", "UPDATE_INQUIRY", "SupportInquiry", id,
                    "Support inquiry status set to " + status, authentication);
            redirectAttributes.addFlashAttribute("success", "Support inquiry updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update inquiry: " + safeMessage(ex));
        }
        return "redirect:/admin/feedback";
    }

    @PostMapping("/admin/contact-messages/{id}/respond")
    public String respondContactMessage(@PathVariable Long id,
                                        @RequestParam String response,
                                        @RequestParam ContactMessageStatus status,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("Response is required");
            }
            ContactMessage message = contactMessageService.get(id);
            message.setResponse(response.trim());
            message.setStatus(status);
            message.setRespondedAt(LocalDateTime.now());
            contactMessageService.save(message);
            auditService.log("SUPPORT", "RESPOND_CONTACT", "ContactMessage", id,
                    "Website contact message status set to " + status, authentication);
            try {
                mailService.sendSupportResponse(message.getEmail(), message.getSubject(), message.getResponse());
            } catch (Exception ignored) {
                // Response remains safely stored in the database even when SMTP is unavailable.
            }
            redirectAttributes.addFlashAttribute("success", "Contact message response saved.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not respond to contact message: " + safeMessage(ex));
        }
        return "redirect:/admin/feedback";
    }

    @GetMapping("/user/feedback")
    public String myFeedback(Model model, Authentication authentication) {
        User user = userService.getCurrent(authentication);
        var feedbacks = feedbackService.mine(user);
        Set<Long> usedOrderIds = feedbacks.stream()
                .filter(feedback -> feedback.getOrder() != null)
                .map(feedback -> feedback.getOrder().getId())
                .collect(Collectors.toSet());

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("complaints", complaintService.mine(user));
        model.addAttribute("inquiries", supportInquiryService.mine(user));
        model.addAttribute("allOrders", orderService.mine(user));
        model.addAttribute("availableOrders", orderService.mine(user).stream()
                .filter(order -> !usedOrderIds.contains(order.getId()))
                .toList());
        return "user/feedback";
    }

    @PostMapping("/user/feedback/save")
    public String saveFeedback(@RequestParam Long orderId,
                               @RequestParam String subject,
                               @RequestParam String message,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            validateText(subject, message);
            User user = userService.getCurrent(authentication);
            Order order = orderService.getForUser(orderId, user);
            if (feedbackService.existsForOrder(order)) {
                throw new IllegalArgumentException("You have already submitted feedback for this order");
            }

            Feedback saved = feedbackService.save(Feedback.builder()
                    .user(user)
                    .order(order)
                    .subject(subject.trim())
                    .message(message.trim())
                    .status(FeedbackStatus.NEW)
                    .build());
            supportRequestSubject.notifyObservers(new SupportRequestEvent(
                    "Feedback", saved.getId(), user.getFullName(), user.getEmail(),
                    saved.getSubject(), saved.getMessage()
            ));

            redirectAttributes.addFlashAttribute("success", "Feedback submitted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not submit feedback: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/feedback/{id}/update")
    public String updateFeedback(@PathVariable Long id,
                                 @RequestParam String subject,
                                 @RequestParam String message,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            validateText(subject, message);
            Feedback feedback = feedbackService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(feedback.getUser(), me);

            feedback.setSubject(subject.trim());
            feedback.setMessage(message.trim());
            feedbackService.save(feedback);
            redirectAttributes.addFlashAttribute("success", "Feedback updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update feedback: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/feedback/{id}/delete")
    public String deleteFeedback(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Feedback feedback = feedbackService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(feedback.getUser(), me);
            feedbackService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Feedback deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete feedback: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/complaints/save")
    public String saveComplaint(@RequestParam String complaintType,
                                @RequestParam String description,
                                @RequestParam(required = false) Long orderId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String cleanType = InputValidation.requireText(complaintType, "Complaint type", 2, 80);
            String cleanDescription = InputValidation.requireText(description, "Complaint description", 5, 2000);

            User user = userService.getCurrent(authentication);
            Order order = orderId == null ? null : orderService.getForUser(orderId, user);

            Complaint saved = complaintService.save(Complaint.builder()
                    .user(user)
                    .order(order)
                    .complaintType(cleanType)
                    .description(cleanDescription)
                    .status(ComplaintStatus.OPEN)
                    .build());
            supportRequestSubject.notifyObservers(new SupportRequestEvent(
                    "Complaint", saved.getId(), user.getFullName(), user.getEmail(),
                    saved.getComplaintType(), saved.getDescription()
            ));

            redirectAttributes.addFlashAttribute("success", "Complaint submitted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not submit complaint: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/complaints/{id}/update")
    public String updateComplaint(@PathVariable Long id,
                                  @RequestParam String complaintType,
                                  @RequestParam String description,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Complaint complaint = complaintService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(complaint.getUser(), me);

            if (complaint.getStatus() == ComplaintStatus.RESOLVED
                    || complaint.getStatus() == ComplaintStatus.CLOSED) {
                throw new IllegalArgumentException("Resolved or closed complaints cannot be edited");
            }

            complaint.setComplaintType(InputValidation.requireText(complaintType, "Complaint type", 2, 80));
            complaint.setDescription(InputValidation.requireText(description, "Complaint description", 5, 2000));
            complaintService.save(complaint);
            redirectAttributes.addFlashAttribute("success", "Complaint updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update complaint: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/complaints/{id}/delete")
    public String deleteComplaint(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Complaint complaint = complaintService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(complaint.getUser(), me);

            if (complaint.getStatus() != ComplaintStatus.OPEN) {
                throw new IllegalArgumentException("Only open complaints can be deleted");
            }

            complaintService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Complaint deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete complaint: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/inquiries/save")
    public String saveInquiry(@RequestParam String subject,
                              @RequestParam String message,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            validateText(subject, message);
            User user = userService.getCurrent(authentication);

            SupportInquiry saved = supportInquiryService.save(SupportInquiry.builder()
                    .user(user)
                    .subject(subject.trim())
                    .message(message.trim())
                    .status(InquiryStatus.NEW)
                    .build());
            supportRequestSubject.notifyObservers(new SupportRequestEvent(
                    "SupportInquiry", saved.getId(), user.getFullName(), user.getEmail(),
                    saved.getSubject(), saved.getMessage()
            ));

            redirectAttributes.addFlashAttribute("success", "Support inquiry submitted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not submit inquiry: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/inquiries/{id}/update")
    public String updateInquiry(@PathVariable Long id,
                                @RequestParam String subject,
                                @RequestParam String message,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            validateText(subject, message);
            SupportInquiry inquiry = supportInquiryService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(inquiry.getUser(), me);

            if (inquiry.getStatus() != InquiryStatus.NEW) {
                throw new IllegalArgumentException("Only new inquiries can be edited");
            }

            inquiry.setSubject(subject.trim());
            inquiry.setMessage(message.trim());
            supportInquiryService.save(inquiry);
            redirectAttributes.addFlashAttribute("success", "Support inquiry updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not update inquiry: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    @PostMapping("/user/inquiries/{id}/delete")
    public String deleteInquiry(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            SupportInquiry inquiry = supportInquiryService.get(id);
            User me = userService.getCurrent(authentication);
            ensureOwner(inquiry.getUser(), me);

            if (inquiry.getStatus() != InquiryStatus.NEW) {
                throw new IllegalArgumentException("Only new inquiries can be deleted");
            }

            supportInquiryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Support inquiry deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not delete inquiry: " + safeMessage(ex));
        }
        return "redirect:/user/feedback";
    }

    private void validateText(String subject, String message) {
        InputValidation.requireText(subject, "Subject", 2, 120);
        InputValidation.requireText(message, "Message", 5, 2000);
    }

    private void ensureOwner(User owner, User currentUser) {
        if (owner == null || currentUser == null || !owner.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You can only manage your own records");
        }
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
