package com.crifigvic.hsrwiki.dto;

import com.crifigvic.hsrwiki.entity.Modifier;
import com.crifigvic.hsrwiki.entity.Piece;

import java.util.List;
import java.util.Map;

public record RelicDto(
        String name,
        Map<String, Piece> pieces,
        List<String>desc,
        List<List<Modifier>> modifiers
) {
}
