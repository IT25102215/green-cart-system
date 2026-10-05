package com.greencart.service;

import com.greencart.entity.SiteReview;
import com.greencart.entity.SiteReviewStatus;
import com.greencart.entity.User;
import com.greencart.repository.SiteReviewRepository;
import com.greencart.util.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiteReviewService {
    private final SiteReviewRepository repo;

    public List<SiteReview> all() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<SiteReview> approved() {
        return repo.findByStatusOrderByCreatedAtDesc(SiteReviewStatus.APPROVED);
    }

    public Optional<SiteReview> mine(User user) {
        return repo.findByUser(user);
    }

    public SiteReview get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Site review not found"));
    }

    public long pendingCount() {
        return repo.countByStatus(SiteReviewStatus.PENDING);
    }

    @Transactional
    public SiteReview submitOrUpdate(User user, Integer rating, String comment) {
        if (user == null) throw new IllegalArgumentException("Please login to submit a review");
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }
        String cleanComment = InputValidation.requireText(comment, "Review", 5, 1000);

        SiteReview review = repo.findByUser(user).orElseGet(() -> SiteReview.builder()
                .user(user)
                .createdAt(LocalDateTime.now())
                .build());

        review.setRating(rating);
        review.setComment(cleanComment);
        // Any new submission/edit must be approved again before it is public.
        review.setStatus(SiteReviewStatus.PENDING);
        review.setModeratedAt(null);
        review.setModeratedBy(null);
        return repo.save(review);
    }

    @Transactional
    public SiteReview approve(Long id, String adminEmail) {
        SiteReview review = get(id);
        review.setStatus(SiteReviewStatus.APPROVED);
        review.setModeratedAt(LocalDateTime.now());
        review.setModeratedBy(adminEmail);
        return repo.save(review);
    }

    @Transactional
    public SiteReview reject(Long id, String adminEmail) {
        SiteReview review = get(id);
        review.setStatus(SiteReviewStatus.REJECTED);
        review.setModeratedAt(LocalDateTime.now());
        review.setModeratedBy(adminEmail);
        return repo.save(review);
    }

    @Transactional
    public void deleteOwn(User user) {
        repo.findByUser(user).ifPresent(repo::delete);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
