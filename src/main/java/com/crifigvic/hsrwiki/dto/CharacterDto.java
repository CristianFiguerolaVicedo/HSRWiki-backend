package com.crifigvic.hsrwiki.dto;

import com.crifigvic.hsrwiki.entity.Ascension;
import com.crifigvic.hsrwiki.entity.Eidolon;
import com.crifigvic.hsrwiki.entity.Skill;
import com.crifigvic.hsrwiki.entity.Trace;

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
        Trace traces
) {
}
