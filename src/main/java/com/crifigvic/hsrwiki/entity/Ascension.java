package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ascension {
    private StatValue hp;
    private StatValue atk;
    private StatValue def;
    private StatValue spd;
    private StatValue taunt;
    private StatValue critRate;
    private StatValue critDmg;
}
