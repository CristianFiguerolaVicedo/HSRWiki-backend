package com.crifigvic.hsrwiki.dto;

import com.crifigvic.hsrwiki.entity.*;

import java.util.List;

public record CharacterDto(
        int id,
        String name,
        int rarity,
        String element,
        String path,
        String icon,
        String splash,
        String miniIcon,
        List<Ascension> ascension,
        List<Eidolon> eidolons,
        Skill skills,
        Trace traces,
        CharBuild build
) {
}
