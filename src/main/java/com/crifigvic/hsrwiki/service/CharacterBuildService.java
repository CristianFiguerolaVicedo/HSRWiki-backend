package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.entity.CharBuild;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class CharacterBuildService {

    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, CharBuild> characterBuilds = new HashMap<>();

    @PostConstruct
    public void loadBuildData() {
        // Try multiple possible locations
        String[] possiblePaths = {
                "src/main/java/com/crifigvic/hsrwiki/util/character_builds.json",  // Your actual path
                "hsr-data/output/character_builds.json",
                "character_builds.json",
                "src/main/resources/character_builds.json"
        };

        boolean loaded = false;
        for (String pathStr : possiblePaths) {
            java.nio.file.Path path = Paths.get(pathStr);

            if (path.toFile().exists()) {
                Map<String, CharBuild> builds = mapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CharBuild>>() {}
                );
                characterBuilds.putAll(builds);
                loaded = true;
                break;
            }
        }
    }

    public CharBuild getBuildForCharacter(String characterName) {
        CharBuild build = characterBuilds.get(characterName);

        return build;
    }

    public Set<String> getCharactersWithBuilds() {
        return characterBuilds.keySet();
    }

    public int getBuildCount() {
        return characterBuilds.size();
    }
}