package com.crifigvic.hsrwiki.dto;

import java.util.List;

public record ItemDto(
        String id,
        String name,
        String type,
        String sub_type,
        Integer rarity,
        String icon,
        List<String>come_from
) {
}
