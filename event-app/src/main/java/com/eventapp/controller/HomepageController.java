package com.eventapp.controller;

import com.eventapp.dto.CategoryDto;
import com.eventapp.dto.EventDto;
import com.eventapp.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Homepage — all data comes from Redis.
 * This controller must NEVER inject EventRepository or call PostgreSQL.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HomepageController {

    private final RedisService redisService;

    @GetMapping("/")
    public String homepage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            Model model) {

        List<EventDto> events = redisService.getCachedEvents();
        List<CategoryDto> categories = redisService.getCachedCategories();

        // Filter by category if requested (client-side filtering on cached data)
        if (categoryId != null) {
            events = events.stream()
                .filter(e -> categoryId.equals(e.getCategoryId()))
                .collect(Collectors.toList());
        }

        // Simple search filter on cached data (no DB hit)
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            events = events.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(q)
                          || (e.getDescription() != null && e.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        }

        boolean cachePopulated = redisService.isCachePopulated();
        Long cacheTtl = redisService.getCacheTtl();

        model.addAttribute("events", events);
        model.addAttribute("categories", categories);
        model.addAttribute("cachePopulated", cachePopulated);
        model.addAttribute("cacheTtl", cacheTtl);
        model.addAttribute("eventCount", events.size());
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("search", search);

        log.debug("Homepage served {} events from Redis (cache populated: {})", events.size(), cachePopulated);
        return "homepage/index";
    }
}