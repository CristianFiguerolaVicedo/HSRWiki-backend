package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * The in-game Character Archive: the "Character Details" text, the Character Stories in order
 * and the voice actors by language. Texts keep only <i> and <b> markup.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CharacterLore {

    private String description;
    private List<String> stories;
    private Map<String, String> voiceActors;
}
