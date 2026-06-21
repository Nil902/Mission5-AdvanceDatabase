package com.eventapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_event_date", columnList = "event_date"),
    @Index(name = "idx_events_is_active", columnList = "is_active"),
    @Index(name = "idx_events_category_id", columnList = "category_id"),
    @Index(name = "idx_events_active_date", columnList = "is_active, event_date"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event implements Serializable {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, length = 255)
    private String title;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @Column(length = 255)
    private String location;
 
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
 
    @Column(name = "image_url", length = 500)
    private String imageUrl;
 
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
 
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
 
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}