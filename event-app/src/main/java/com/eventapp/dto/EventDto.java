package com.eventapp.dto;
 
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
 
/**
 * Flat DTO stored in Redis — no JPA proxies, safe to serialize.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto implements Serializable {
 
    private Long id;
    private String title;
    private String description;
    private String location;
 
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime eventDate;
 
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private Boolean isActive;
 
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;
}