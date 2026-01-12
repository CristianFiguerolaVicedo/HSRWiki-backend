package com.crifigvic.hsrwiki.dto;

import com.crifigvic.hsrwiki.entity.LightconeAbility;
import com.crifigvic.hsrwiki.entity.LightconeAscension;

import java.util.List;

public record LightconeDto(
        Integer id,
        String name,
        Integer rarity,
        com.crifigvic.hsrwiki.util.Path path,
        String desc,
        String icon,
        String image,
        String miniIcon,
        List<LightconeAscension> ascension,
        LightconeAbility ability
) {

}
