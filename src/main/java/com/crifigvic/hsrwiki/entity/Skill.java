package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Skill {

    private BasicSkill basic;
    private ActiveSkill skill;
    private UltimateSkill ult;
    private TalentSkill talent;
    private List<ExtraSkill> extra;
}
