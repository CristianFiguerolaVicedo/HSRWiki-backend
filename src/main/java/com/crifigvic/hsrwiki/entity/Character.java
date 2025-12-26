package com.crifigvic.hsrwiki.entity;

import com.crifigvic.hsrwiki.util.Element;
import com.crifigvic.hsrwiki.util.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Character {

    private Integer id;

    private String name;

    private int rarity;

    private Element element;

    private Path path;

    private String icon;
    private String role;
    private String tier;
}
