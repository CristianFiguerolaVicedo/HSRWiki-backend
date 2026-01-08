package com.crifigvic.hsrwiki.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CharBuild {
    private String charName;
    @JsonProperty("lightCones")
    private List<LightCone> lightCones;

    @JsonProperty("relicSets")
    private List<RelicSet> relicSets;

    @JsonProperty("planarSets")
    private List<PlanarSet> planarSets;

    @JsonProperty("mainStats")
    private MainStats mainStats;

    @JsonProperty("subStats")
    private SubStats subStats;

    @JsonProperty("teams")
    private List<TeamCompositions> teams;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LightCone {
        private String name;
        private Integer rarity;
        private String description;
        private Integer priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RelicSet {
        private String name;
        private String description;
        private Integer priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PlanarSet {
        private String name;
        private String description;
        private Integer priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MainStats {
        private String body;
        private String feet;
        private String sphere;
        private String rope;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SubStats {
        private List<String> priority;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TeamCompositions {
        private String name;
        private String description;
        private List<String> teamMembers;
    }
}
