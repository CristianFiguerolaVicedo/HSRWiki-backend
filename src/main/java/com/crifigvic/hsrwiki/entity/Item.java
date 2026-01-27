package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    private String id;
    private String name;
    private String type;
    private String sub_type;
    private Integer rarity;
    private String icon;
    private List<String> come_from;
}
