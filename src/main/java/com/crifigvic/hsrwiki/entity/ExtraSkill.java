package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Skills beyond basic/skill/ult/talent: Memosprite skills, Elation skills, alternate skills. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtraSkill {
    private String name;
    /** In-game label, e.g. "Memosprite Skill", "Elation Skill", "Skill". */
    private String type;
    private Integer maxLevel;
    private String desc;
    private List<List<String>> params;
}
