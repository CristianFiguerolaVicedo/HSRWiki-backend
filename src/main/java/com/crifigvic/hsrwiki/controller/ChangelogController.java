package com.crifigvic.hsrwiki.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * What changed each week (data/changelog.json, written by data-pipeline/changelog.py from the weekly
 * data update and the builds review). Served as-is for the website's Updates page.
 */
@Slf4j
@RestController
@RequestMapping("/api/changelog")
public class ChangelogController {

    private final String changelogJson;

    public ChangelogController(@Value("${hsrwiki.changelog-file:classpath:data/changelog.json}") Resource file) {
        String json;
        try (InputStream in = file.getInputStream()) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("No changelog found ({}): the Updates page will be empty", e.getMessage());
            json = "{\"entries\":[]}";
        }
        this.changelogJson = json;
    }

    @GetMapping
    public ResponseEntity<String> getChangelog() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                .body(changelogJson);
    }
}
