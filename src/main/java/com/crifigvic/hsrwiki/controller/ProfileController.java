package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.config.RateLimiter;
import com.crifigvic.hsrwiki.dto.ProfileDto;
import com.crifigvic.hsrwiki.service.ShowcaseService;
import com.crifigvic.hsrwiki.service.ShowcaseService.ShowcaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/** A player's public showcase by UID (no login). Errors come back as {"error": "..."} with a matching status. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ShowcaseService showcaseService;

    /** Lookups per visitor per minute: plenty for a person, a brake for a script. */
    private final RateLimiter perVisitor = new RateLimiter(10, Duration.ofMinutes(1), 10_000);

    @GetMapping("/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid, HttpServletRequest request) {
        if (!perVisitor.tryAcquire(clientIp(request))) {
            return ResponseEntity.status(429).body(Map.of("error", "Too many lookups. Wait a minute and try again."));
        }
        try {
            ProfileDto profile = showcaseService.getProfile(uid.trim());
            return ResponseEntity.ok(profile);
        } catch (ShowcaseException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * The visitor's address. Render serves the API through Cloudflare, which sets CF-Connecting-IP
     * itself (a client can't choose it); otherwise the first X-Forwarded-For entry, otherwise the
     * connection's address. A forged header only changes which bucket a request counts against:
     * the server-wide limit on Enka calls in ShowcaseService still applies.
     */
    static String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (StringUtils.hasText(cf)) {
            return cf.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
