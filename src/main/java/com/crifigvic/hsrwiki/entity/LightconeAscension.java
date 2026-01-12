package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LightconeAscension {
    private StatValue hp;
    private StatValue atk;
    private StatValue def;
}
