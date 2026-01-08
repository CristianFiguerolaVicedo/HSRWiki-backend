package com.crifigvic.hsrwiki.dto;

import com.crifigvic.hsrwiki.entity.*;

import java.lang.reflect.Array;
import java.util.List;

public record CharacterDto(
        int id,
        String name,
        int rarity,
        String element,
        String path,
        String icon,
        List<Ascension> ascension,
        List<Eidolon> eidolons,
        Skill skills,
        Trace traces,
        CharBuild build
) {
}
