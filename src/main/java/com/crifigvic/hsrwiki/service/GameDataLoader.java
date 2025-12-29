package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.util.Element;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.crifigvic.hsrwiki.entity.Character;
import com.crifigvic.hsrwiki.util.Path;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class GameDataLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private Map<Integer, Character> characters = new HashMap<>();

    @PostConstruct
    public void load() throws IOException {
        java.nio.file.Path path = Paths.get("hsr-data/output/game_data_verbose.json");
        JsonNode root = mapper.readTree(path.toFile());

        JsonNode charsNode = root.get("characters");

        for (Iterator<String> it = charsNode.propertyNames().iterator(); it.hasNext(); ) {

            String name = it.next();
            JsonNode node = charsNode.get(name);

            if (!node.has("rarity")) continue;

            String elementRaw = getText(node, "element");
            String pathRaw = getText(node, "path");

            if (elementRaw == null || pathRaw == null) continue;

            Character character = Character.builder()
                    .id(name.hashCode())
                    .name(name)
                    .rarity(node.get("rarity").asInt())
                    .element(Element.valueOf(elementRaw.toUpperCase()))
                    .path(Path.valueOf(
                            pathRaw.replace("The ", "").toUpperCase()
                    ))
                    .icon(buildIconPath(name))
                    .build();

            characters.put(character.getId(), character);
        }
    }

    public Collection<Character> getAllCharacters() {
        return characters.values();
    }

    public Character getById(int id) {
        return characters.get(id);
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asText() : null;
    }

    private String buildIconPath(String characterName) {
        return "icons/mini/" + characterName + ".png";
    }
}
