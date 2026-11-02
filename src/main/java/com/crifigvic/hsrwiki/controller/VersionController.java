package com.crifigvic.hsrwiki.controller;

import com.crifigvic.hsrwiki.service.GameDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VersionController {

    private final GameDataLoader loader;

    /** Game version the loaded data was generated from, e.g. {"gameVersion": "4.6.0"}. */
    @GetMapping("/api/version")
    public Map<String, String> getVersion() {
        return Map.of("gameVersion", loader.getGameVersion() == null ? "unknown" : loader.getGameVersion());
    }
}
