package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Which characters and light cones need an item to ascend or level up their traces. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemUsage {
    private List<Ref> characters;
    private List<Ref> lightCones;
    /** True for materials nearly everyone uses (e.g. Credit); the lists are left empty then. */
    private boolean everyone;

    /** A link to a character or light cone page. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Ref {
        private Integer id;
        private String name;
        private String icon;
    }
}
