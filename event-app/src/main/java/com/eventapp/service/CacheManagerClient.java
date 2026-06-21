package com.eventapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client that calls the Python Cache Manager (Server 03)
 * to refresh Redis after admin creates/updates/deletes events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagerClient {

    @Value("${cache.manager.url}")
    private String cacheManagerUrl;

    private final WebClient.Builder webClientBuilder;

    /**
     * Tell Server 03 to pull fresh data from PostgreSQL and push to Redis.
     */
    public void triggerCacheRefresh() {
        try {
            String result = webClientBuilder.build()
                .post()
                .uri(cacheManagerUrl + "/refresh")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> {
                    log.error("Cache refresh failed: {}", e.getMessage());
                    return Mono.just("error");
                })
                .block();

            log.info("Cache Manager response: {}", result);
        } catch (Exception e) {
            log.error("Could not reach Cache Manager: {}", e.getMessage());
        }
    }

    /**
     * Tell Server 03 to clear Redis entirely.
     */
    public void triggerCacheInvalidate() {
        try {
            webClientBuilder.build()
                .post()
                .uri(cacheManagerUrl + "/invalidate")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> Mono.just("error"))
                .block();

            log.info("Cache invalidated via Cache Manager");
        } catch (Exception e) {
            log.error("Could not reach Cache Manager for invalidation: {}", e.getMessage());
        }
    }

    /**
     * Health check — used on admin panel to show Server 03 status.
     */
    public boolean isCacheManagerHealthy() {
        try {
            String resp = webClientBuilder.build()
                .get()
                .uri(cacheManagerUrl + "/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> Mono.just("down"))
                .block();
            return !"down".equals(resp);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Trigger pgBadger report generation on Server 03.
     */
    public boolean generatePgBadgerReport() {
        try {
            String resp = webClientBuilder.build()
                .post()
                .uri(cacheManagerUrl + "/report/generate")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.error("pgBadger report generation failed: {}", e.getMessage());
                    return Mono.just("error");
                })
                .block();
            return resp != null && !resp.equals("error");
        } catch (Exception e) {
            log.error("Could not reach Cache Manager for report: {}", e.getMessage());
            return false;
        }
    }

    public boolean isReportAvailable() {
        try {
            String resp = webClientBuilder.build()
                .get()
                .uri(cacheManagerUrl + "/report/view")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> Mono.empty())
                .block();
            return resp != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getReportViewUrl() {
        return cacheManagerUrl + "/report/view";
    }
}