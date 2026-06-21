package com.eventapp.controller;

import com.eventapp.model.Category;
import com.eventapp.model.Event;
import com.eventapp.service.CacheManagerClient;
import com.eventapp.service.EventService;
import com.eventapp.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final EventService eventService;
    private final RedisService redisService;
    private final CacheManagerClient cacheManagerClient;

    // ─── Dashboard ────────────────────────────────────────────────

    @GetMapping("")
    public String dashboard(Model model) {
        model.addAttribute("events", eventService.getAllEventsForAdmin());
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("cachePopulated", redisService.isCachePopulated());
        model.addAttribute("cacheTtl", redisService.getCacheTtl());
        model.addAttribute("cacheManagerHealthy", cacheManagerClient.isCacheManagerHealthy());
        return "admin/dashboard";
    }

    // ─── Event Create ─────────────────────────────────────────────

    @GetMapping("/events/new")
    public String newEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("isEdit", false);
        return "admin/event-form";
    }

    @PostMapping("/events/create")
    public String createEvent(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String location,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime eventDate,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "true") Boolean isActive,
            RedirectAttributes ra) {

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setEventDate(eventDate);
        event.setCategory(eventService.getCategoryById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found")));
        event.setImageUrl(imageUrl);
        event.setIsActive(isActive);

        eventService.createEvent(event);
        ra.addFlashAttribute("success", "Event created and cache refreshed.");
        return "redirect:/admin";
    }

    // ─── Event Edit ───────────────────────────────────────────────

    @GetMapping("/events/{id}/edit")
    public String editEventForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id)
            .orElseThrow(() -> new RuntimeException("Event not found"));
        model.addAttribute("event", event);
        model.addAttribute("categories", eventService.getAllCategories());
        model.addAttribute("isEdit", true);
        return "admin/event-form";
    }

    @PostMapping("/events/{id}/update")
    public String updateEvent(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String location,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime eventDate,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(defaultValue = "false") Boolean isActive,
            RedirectAttributes ra) {

        Event updated = new Event();
        updated.setTitle(title);
        updated.setDescription(description);
        updated.setLocation(location);
        updated.setEventDate(eventDate);
        updated.setCategory(eventService.getCategoryById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found")));
        updated.setImageUrl(imageUrl);
        updated.setIsActive(isActive);

        eventService.updateEvent(id, updated);
        ra.addFlashAttribute("success", "Event updated and cache refreshed.");
        return "redirect:/admin";
    }

    // ─── Event Delete / Toggle ────────────────────────────────────

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes ra) {
        eventService.deleteEvent(id);
        ra.addFlashAttribute("success", "Event deleted and cache refreshed.");
        return "redirect:/admin";
    }

    @PostMapping("/events/{id}/toggle")
    public String toggleEvent(@PathVariable Long id, RedirectAttributes ra) {
        eventService.toggleActive(id);
        ra.addFlashAttribute("success", "Event status toggled and cache refreshed.");
        return "redirect:/admin";
    }

    // ─── Category CRUD ────────────────────────────────────────────

    @PostMapping("/categories/create")
    public String createCategory(@RequestParam String name,
                                  @RequestParam String description,
                                  RedirectAttributes ra) {
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(description);
        eventService.createCategory(cat);
        ra.addFlashAttribute("success", "Category created.");
        return "redirect:/admin";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        eventService.deleteCategory(id);
        ra.addFlashAttribute("success", "Category deleted.");
        return "redirect:/admin";
    }

    // ─── Manual Cache Controls ────────────────────────────────────

    @PostMapping("/cache/refresh")
    public String refreshCache(RedirectAttributes ra) {
        eventService.manualCacheRefresh();
        ra.addFlashAttribute("success", "Cache refresh triggered via Cache Manager.");
        return "redirect:/admin";
    }

    @PostMapping("/cache/invalidate")
    public String invalidateCache(RedirectAttributes ra) {
        cacheManagerClient.triggerCacheInvalidate();
        ra.addFlashAttribute("warning", "Redis cache cleared. Refresh to rebuild.");
        return "redirect:/admin";
    }

    // ─── pgBadger Report ──────────────────────────────────────────

    @PostMapping("/report/generate")
    public String generateReport(RedirectAttributes ra) {
        boolean success = cacheManagerClient.generatePgBadgerReport();
        if (success) {
            ra.addFlashAttribute("success", "pgBadger report generated successfully.");
        } else {
            ra.addFlashAttribute("warning", "Failed to generate pgBadger report.");
        }
        return "redirect:/admin";
    }

}