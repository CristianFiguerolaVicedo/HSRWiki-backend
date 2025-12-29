package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Trace {

    private TechniqueTrace technique;
    private Map<String, AbilityTrace> abilities;
    private Map<String, StatTrace> stats;
}
