package com.eventapp.service;

import com.eventapp.dto.EventDto;
import com.eventapp.model.Category;
import com.eventapp.model.Event;
import com.eventapp.repository.CategoryRepository;
import com.eventapp.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CacheManagerClient cacheManagerClient;

    // ─── Admin CRUD (writes to PostgreSQL only) ────────────────────

    @Transactional
    public Event createEvent(Event event) {
        Event saved = eventRepository.save(event);
        log.info("Event created [id={}], triggering cache refresh", saved.getId());
        // After DB write → ask Cache Manager to refresh Redis
        cacheManagerClient.triggerCacheRefresh();
        return saved;
    }

    @Transactional
    public Event updateEvent(Long id, Event updated) {
        Event existing = eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLocation(updated.getLocation());
        existing.setEventDate(updated.getEventDate());
        existing.setCategory(updated.getCategory());
        existing.setImageUrl(updated.getImageUrl());
        existing.setIsActive(updated.getIsActive());

        Event saved = eventRepository.save(existing);
        log.info("Event updated [id={}], triggering cache refresh", id);
        cacheManagerClient.triggerCacheRefresh();
        return saved;
    }

    @Transactional
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
        log.info("Event deleted [id={}], triggering cache refresh", id);
        cacheManagerClient.triggerCacheRefresh();
    }

    @Transactional
    public void toggleActive(Long id) {
        Event event = eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        event.setIsActive(!event.getIsActive());
        eventRepository.save(event);
        cacheManagerClient.triggerCacheRefresh();
    }

    // ─── Read (admin panel — can read directly from PostgreSQL) ────

    public List<Event> getAllEventsForAdmin() {
        return eventRepository.findAllWithCategory();
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // ─── Category CRUD ─────────────────────────────────────────────

    @Transactional
    public Category createCategory(Category category) {
        Category saved = categoryRepository.save(category);
        cacheManagerClient.triggerCacheRefresh();
        return saved;
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
        cacheManagerClient.triggerCacheRefresh();
    }

    // ─── Manual cache refresh (admin button) ──────────────────────

    public void manualCacheRefresh() {
        log.info("Manual cache refresh triggered by admin");
        cacheManagerClient.triggerCacheRefresh();
    }
}