package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.entity.CharBuild;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Hand-written recommended builds, keyed by character name (src/main/resources/data/character_builds.json). */
@Service
@Slf4j
public class CharacterBuildService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CharBuild> characterBuilds = new HashMap<>();

    @Value("${hsrwiki.builds-file:classpath:data/character_builds.json}")
    private String buildsFile;

    @PostConstruct
    public void loadBuildData() throws IOException {
        Resource resource = new DefaultResourceLoader().getResource(buildsFile);
        if (!resource.exists()) {
            log.warn("No character builds found at {}", buildsFile);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            characterBuilds.putAll(mapper.readValue(in, new TypeReference<Map<String, CharBuild>>() {}));
        }
        log.info("Loaded {} character builds", characterBuilds.size());
    }

    public CharBuild getBuildForCharacter(String characterName) {
        return characterBuilds.get(characterName);
    }

    public Set<String> getCharactersWithBuilds() {
        return characterBuilds.keySet();
    }

    public int getBuildCount() {
        return characterBuilds.size();
    }
}
