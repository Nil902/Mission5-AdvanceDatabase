package com.eventapp.repository;

import com.eventapp.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Used by Cache Manager: fetch all active events ordered by date
    // Uses idx_events_active_date composite index
    @Query("SELECT e FROM Event e JOIN FETCH e.category WHERE e.isActive = true ORDER BY e.eventDate ASC")
    List<Event> findAllActiveOrderByDate();

    // Used by admin panel
    @Query("SELECT e FROM Event e JOIN FETCH e.category ORDER BY e.createdAt DESC")
    List<Event> findAllWithCategory();

    // Filter by category — uses idx_events_category_id
    @Query("SELECT e FROM Event e JOIN FETCH e.category WHERE e.category.id = :categoryId AND e.isActive = true ORDER BY e.eventDate ASC")
    List<Event> findByCategoryIdAndActive(Long categoryId);
}