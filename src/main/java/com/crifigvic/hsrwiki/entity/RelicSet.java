package com.crifigvic.hsrwiki.entity;

import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelicSet {
    private String name;
    private Map<String, Piece> pieces;
    private List<String> desc;
    private List<List<Modifier>> modifiers;
}
