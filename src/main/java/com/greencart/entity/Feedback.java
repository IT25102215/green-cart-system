package com.greencart.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="feedback") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id",nullable=false) private User user;
 @OneToOne(fetch=FetchType.EAGER) @JoinColumn(name="order_id", unique=true) private Order order;
 @Column(nullable=false,length=120) private String subject;
 @Column(nullable=false,length=2000) private String message;
 @Column(length=2000) private String response;
 @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private FeedbackStatus status=FeedbackStatus.NEW;
 @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
 @Builder.Default private LocalDateTime updatedAt=LocalDateTime.now();
 @PreUpdate public void touch(){updatedAt=LocalDateTime.now();}
}
