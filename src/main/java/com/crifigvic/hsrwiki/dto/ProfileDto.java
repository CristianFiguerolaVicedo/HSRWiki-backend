package com.crifigvic.hsrwiki.dto;

import java.util.List;

/**
 * A player's public in-game showcase (from Enka.Network), translated to wiki data.
 * No login is involved: this is the information the player chose to show in their profile.
 */
public record ProfileDto(
        String uid,
        String nickname,
        int level,
        int equilibriumLevel,
        String signature,
        int characterCount,
        int achievementCount,
        List<ShowcaseCharacter> characters,
        /* seconds until Enka refreshes this profile */
        int ttl
) {

    public record ShowcaseCharacter(
            int id,
            String name,
            String element,
            String path,
            int rarity,
            String icon,
            String miniIcon,
            int level,
            int eidolon,
            boolean enhanced,
            ShowcaseLightCone lightCone,
            List<ShowcaseRelic> relics,
            /* set name -> pieces equipped, e.g. {"Musketeer of Wild Wheat": 4} */
            List<SetCount> sets,
            BuildCheck buildCheck
    ) {
    }

    public record ShowcaseLightCone(
            Integer id,
            String name,
            Integer rarity,
            String icon,
            int level,
            int superimposition,
            boolean recommended
    ) {
    }

    public record ShowcaseRelic(
            String slot,
            String set,
            int rarity,
            int level,
            String icon,
            Stat mainStat,
            List<Stat> subStats,
            /* only for body/feet/sphere/rope: whether the main stat is the recommended one */
            Boolean recommendedMainStat
    ) {
    }

    /** value is raw: 0.324 for a 32.4% stat, 25.0 for flat SPD */
    public record Stat(String type, String name, double value, boolean percent) {
    }

    public record SetCount(String set, int pieces, boolean planar, boolean recommended) {
    }

    /** Comparison with the wiki's recommended build; null when the character has no build yet */
    public record BuildCheck(boolean lightCone, boolean relicSet, boolean planarSet, int mainStatsMatched, int mainStatsChecked) {
    }
}
