package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.dto.ProfileDto;
import com.crifigvic.hsrwiki.dto.ProfileDto.*;
import com.crifigvic.hsrwiki.entity.CharBuild;
import com.crifigvic.hsrwiki.entity.Character;
import com.crifigvic.hsrwiki.entity.Lightcone;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads a player's public showcase from Enka.Network by UID and translates it to wiki data.
 *
 * No HoYoverse login or credentials are involved: Enka returns the characters the player
 * chose to display in their in-game profile. Responses are cached for the "ttl" Enka returns,
 * as its API rules ask (https://github.com/EnkaNetwork/API-docs).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowcaseService {

    /** Thrown with the HTTP status the API should answer with. */
    public static class ShowcaseException extends RuntimeException {
        private final int status;

        public ShowcaseException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    private static final Set<String> BUILD_SLOTS = Set.of("Body", "Feet", "Planar Sphere", "Link Rope");
    private static final List<String> SLOT_ORDER = List.of("Head", "Hands", "Body", "Feet", "Planar Sphere", "Link Rope");

    private final GameDataLoader gameData;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, CachedProfile> cache = new ConcurrentHashMap<>();

    @Value("${hsrwiki.enka.base-url:https://enka.network/api/hsr/uid}")
    private String enkaBaseUrl;

    @Value("${hsrwiki.enka.user-agent:HSRWiki/1.0 (+https://github.com/CristianFiguerolaVicedo/HSRWiki-backend)}")
    private String userAgent;

    @Value("${hsrwiki.showcase-lookup-file:classpath:data/showcase_lookup.json}")
    private String lookupFile;

    private JsonNode lookup;

    private record CachedProfile(ProfileDto profile, long expiresAt) {
    }

    @PostConstruct
    public void loadLookup() throws IOException {
        Resource resource = new DefaultResourceLoader().getResource(lookupFile);
        if (!resource.exists()) {
            log.warn("Showcase lookup not found at {}; run data-pipeline/update_data.py", lookupFile);
            lookup = mapper.createObjectNode();
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            lookup = mapper.readTree(in);
        }
    }

    public ProfileDto getProfile(String uid) {
        if (uid == null || !uid.matches("\\d{9,10}")) {
            throw new ShowcaseException(400, "A UID is 9 or 10 digits long.");
        }

        CachedProfile cached = cache.get(uid);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached.profile();
        }

        JsonNode root = fetch(uid);
        ProfileDto profile = toProfile(uid, root);
        long ttlMillis = Math.max(60, profile.ttl()) * 1000L;
        cache.put(uid, new CachedProfile(profile, System.currentTimeMillis() + ttlMillis));
        return profile;
    }

    private JsonNode fetch(String uid) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(enkaBaseUrl + "/" + uid))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ShowcaseException(502, "Couldn't reach Enka.Network. Try again in a moment.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ShowcaseException(502, "The request was interrupted.");
        }

        switch (response.statusCode()) {
            case 200 -> {
                return mapper.readTree(response.body());
            }
            case 400 -> throw new ShowcaseException(400, "That doesn't look like a valid UID.");
            case 404 -> throw new ShowcaseException(404, "No player with that UID exists.");
            case 424 -> throw new ShowcaseException(503, "The game is under maintenance. Try again after the update.");
            case 429 -> throw new ShowcaseException(429, "Too many lookups right now. Wait a minute and try again.");
            default -> throw new ShowcaseException(502, "Enka.Network answered with an error (" + response.statusCode() + ").");
        }
    }

    // ---------------------------------------------------------------- translation

    ProfileDto toProfile(String uid, JsonNode root) {
        JsonNode info = root.path("detailInfo");
        JsonNode record = info.path("recordInfo");

        List<ShowcaseCharacter> characters = new ArrayList<>();
        for (JsonNode avatar : info.path("avatarDetailList")) {
            ShowcaseCharacter character = toCharacter(avatar);
            if (character != null) {
                characters.add(character);
            }
        }

        return new ProfileDto(
                uid,
                info.path("nickname").asString(""),
                info.path("level").asInt(0),
                info.path("worldLevel").asInt(0),
                info.path("signature").asString(""),
                record.path("avatarCount").asInt(0),
                record.path("achievementCount").asInt(0),
                characters,
                root.path("ttl").asInt(60)
        );
    }

    private ShowcaseCharacter toCharacter(JsonNode avatar) {
        Character character = gameData.getById(avatar.path("avatarId").asInt(0));
        if (character == null) {
            // released after our data was generated
            return null;
        }
        CharBuild build = character.getBuild();

        ShowcaseLightCone lightCone = toLightCone(avatar.path("equipment"), build);

        List<ShowcaseRelic> relics = new ArrayList<>();
        for (JsonNode relic : avatar.path("relicList")) {
            ShowcaseRelic r = toRelic(relic, build);
            if (r != null) relics.add(r);
        }
        relics.sort(Comparator.comparingInt(r -> SLOT_ORDER.indexOf(r.slot())));

        List<SetCount> sets = countSets(relics, build);

        BuildCheck check = null;
        if (build != null) {
            boolean relicSet = sets.stream().anyMatch(s -> !s.planar() && s.recommended() && s.pieces() >= 2);
            boolean planarSet = sets.stream().anyMatch(s -> s.planar() && s.recommended() && s.pieces() >= 2);
            int checked = (int) relics.stream().filter(r -> r.recommendedMainStat() != null).count();
            int matched = (int) relics.stream().filter(r -> Boolean.TRUE.equals(r.recommendedMainStat())).count();
            check = new BuildCheck(lightCone != null && lightCone.recommended(), relicSet, planarSet, matched, checked);
        }

        return new ShowcaseCharacter(
                character.getId(),
                character.getName(),
                character.getElement().name(),
                character.getPath().name(),
                character.getRarity(),
                character.getIcon(),
                character.getMiniIcon(),
                avatar.path("level").asInt(1),
                avatar.path("promotion").asInt(0),
                avatar.path("rank").asInt(0),
                avatar.path("enhancedId").asInt(0) != 0,
                lightCone,
                relics,
                sets,
                check
        );
    }

    private ShowcaseLightCone toLightCone(JsonNode equipment, CharBuild build) {
        if (equipment.isMissingNode() || !equipment.has("tid")) return null;
        String name = lookup.path("light_cones").path(equipment.path("tid").asString("")).asString(null);
        Lightcone lc = name == null ? null : gameData.getLightconeByName(name);
        boolean recommended = build != null && build.getLightCones() != null && name != null
                && build.getLightCones().stream().anyMatch(c -> name.equals(c.getName()));
        return new ShowcaseLightCone(
                lc == null ? null : lc.getId(),
                name == null ? "Unknown light cone" : name,
                lc == null ? null : lc.getRarity(),
                lc == null ? null : lc.getMiniIcon(),
                equipment.path("level").asInt(1),
                equipment.path("promotion").asInt(0),
                equipment.path("rank").asInt(1),
                recommended
        );
    }

    private ShowcaseRelic toRelic(JsonNode relic, CharBuild build) {
        JsonNode piece = lookup.path("relics").path(relic.path("tid").asString(""));
        if (piece.isMissingNode()) return null;

        int level = relic.path("level").asInt(0);
        String slot = piece.path("slot").asString("");

        JsonNode mainAffix = lookup.path("main_affixes").path(piece.path("main_group").asString(""))
                .path(relic.path("mainAffixId").asString(""));
        Stat mainStat = mainAffix.isMissingNode() ? null
                : stat(mainAffix.path("property").asString(""),
                mainAffix.path("base").asDouble(0) + mainAffix.path("step").asDouble(0) * level);

        List<Stat> subStats = new ArrayList<>();
        JsonNode subGroup = lookup.path("sub_affixes").path(piece.path("sub_group").asString(""));
        for (JsonNode sub : relic.path("subAffixList")) {
            JsonNode affix = subGroup.path(sub.path("affixId").asString(""));
            if (affix.isMissingNode()) continue;
            double value = affix.path("base").asDouble(0) * sub.path("cnt").asInt(1)
                    + affix.path("step").asDouble(0) * sub.path("step").asInt(0);
            subStats.add(stat(affix.path("property").asString(""), value));
        }

        Boolean recommendedMain = null;
        if (BUILD_SLOTS.contains(slot) && mainStat != null && build != null && build.getMainStats() != null) {
            String wanted = switch (slot) {
                case "Body" -> build.getMainStats().getBody();
                case "Feet" -> build.getMainStats().getFeet();
                case "Planar Sphere" -> build.getMainStats().getSphere();
                default -> build.getMainStats().getRope();
            };
            if (wanted != null) {
                recommendedMain = matchesRecommended(mainStat.name(), wanted);
            }
        }

        return new ShowcaseRelic(
                slot,
                piece.path("set").asString(null),
                piece.path("rarity").asInt(5),
                level,
                piece.path("icon").asString(null),
                mainStat,
                subStats,
                recommendedMain
        );
    }

    private Stat stat(String property, double value) {
        JsonNode prop = lookup.path("properties").path(property);
        return new Stat(property, prop.path("name").asString(property), value, prop.path("percent").asBoolean(false));
    }

    private List<SetCount> countSets(List<ShowcaseRelic> relics, CharBuild build) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Boolean> planar = new HashMap<>();
        for (ShowcaseRelic r : relics) {
            if (r.set() == null) continue;
            counts.merge(r.set(), 1, Integer::sum);
            planar.put(r.set(), r.slot().equals("Planar Sphere") || r.slot().equals("Link Rope"));
        }
        Set<String> recommended = new HashSet<>();
        if (build != null) {
            if (build.getRelicSets() != null) build.getRelicSets().forEach(s -> recommended.add(s.getName()));
            if (build.getPlanarSets() != null) build.getPlanarSets().forEach(s -> recommended.add(s.getName()));
        }
        List<SetCount> result = new ArrayList<>();
        counts.forEach((set, n) -> result.add(new SetCount(set, n, planar.get(set), recommended.contains(set))));
        result.sort(Comparator.comparing(SetCount::planar).thenComparing(SetCount::pieces, Comparator.reverseOrder()));
        return result;
    }

    // Build files write main stats loosely ("Energy Regen", "Effect Hit%", "HP% / DEF%")
    private static final Map<String, String> STAT_ALIASES = Map.of(
            "energy regen", "energy regeneration rate",
            "effect hit", "effect hit rate",
            "outgoing healing", "outgoing healing boost"
    );

    private static String normalizeStat(String s) {
        String n = s.toLowerCase(Locale.ROOT).replace("%", "").trim();
        return STAT_ALIASES.getOrDefault(n, n);
    }

    static boolean matchesRecommended(String statName, String recommended) {
        String have = normalizeStat(statName);
        for (String option : recommended.split("/|\\bor\\b")) {
            if (normalizeStat(option).equals(have)) return true;
        }
        return false;
    }
}
