package com.crifigvic.hsrwiki.dto;

public record CharacterDto(
        int id,
        String name,
        int rarity,
        String element,
        String path,
        String icon
) {
}
