package com.crifigvic.hsrwiki.model;

import lombok.Data;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Data
public class GameData {
    private Map<String, JsonNode> characters;
}
