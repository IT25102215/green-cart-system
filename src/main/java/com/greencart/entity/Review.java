package com.greencart.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id") private User user;
  @OneToOne(fetch=FetchType.EAGER) @JoinColumn(name="order_id", unique=true) private Order order;
  @Column(nullable=false) private Integer rating;
  @Column(length=1000) private String comment;
  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
