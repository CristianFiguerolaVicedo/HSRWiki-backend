package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.dto.ProfileDto;
import com.crifigvic.hsrwiki.service.ShowcaseService;
import com.crifigvic.hsrwiki.service.ShowcaseService.ShowcaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** A player's public showcase by UID (no login). Errors come back as {"error": "..."} with a matching status. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class ProfileController {

    private final ShowcaseService showcaseService;

    @GetMapping("/{uid}")
    public ResponseEntity<?> getProfile(@PathVariable String uid) {
        try {
            ProfileDto profile = showcaseService.getProfile(uid.trim());
            return ResponseEntity.ok(profile);
        } catch (ShowcaseException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
        }
    }
}
