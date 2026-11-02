package com.crifigvic.hsrwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The upgraded kit some characters received from version 3.4 onwards (Kafka, Blade, Seele...). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnhancedKit {
    private Skill skills;
    private Trace traces;
}
