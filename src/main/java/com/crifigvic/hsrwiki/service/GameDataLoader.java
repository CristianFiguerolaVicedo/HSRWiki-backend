package com.crifigvic.hsrwiki.service;

import com.crifigvic.hsrwiki.entity.*;
import com.crifigvic.hsrwiki.entity.AbilityTrace;
import com.crifigvic.hsrwiki.entity.TechniqueTrace;
import com.crifigvic.hsrwiki.entity.StatTrace;
import com.crifigvic.hsrwiki.entity.ActiveSkill;
import com.crifigvic.hsrwiki.entity.BasicSkill;
import com.crifigvic.hsrwiki.entity.TalentSkill;
import com.crifigvic.hsrwiki.entity.UltimateSkill;
import com.crifigvic.hsrwiki.entity.Character;
import com.crifigvic.hsrwiki.util.Element;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.crifigvic.hsrwiki.util.Path;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameDataLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private Map<Integer, Character> characters = new HashMap<>();
    private Map<Integer, Lightcone> lightcones = new HashMap<>();
    private Map<String, RelicSet> relicSets = new HashMap<>();
    private final CharacterBuildService characterBuildService;

    @PostConstruct
    public void load() throws IOException {
        java.nio.file.Path path = Paths.get("hsr-data/output/game_data_verbose_with_icons.json");
        JsonNode root = mapper.readTree(path.toFile());

        loadCharacters(root.get("characters"));
        loadLightcones(root.get("light_cones"));
        loadRelicSets(root.get("relic_sets"));
    }

    private void loadRelicSets(JsonNode relicNode) throws IOException {
        if (relicNode == null) return;

        for (Iterator<String> it = relicNode.propertyNames().iterator(); it.hasNext(); ) {
            String name = it.next();
            JsonNode node = relicNode.get(name);

            RelicSet relicSet = parseRelicSet(name, node);
            if (relicSet != null) {
                relicSets.put(name, relicSet);
            }
        }
    }

    private void loadCharacters(JsonNode charsNode) throws IOException {
        if (charsNode == null) return;

        for (Iterator<String> it = charsNode.propertyNames().iterator(); it.hasNext(); ) {
            String name = it.next();
            JsonNode node = charsNode.get(name);

            if (!node.has("rarity")) continue;

            String elementRaw = getText(node, "element");
            String pathRaw = getText(node, "path");

            if (elementRaw == null || pathRaw == null) continue;

            List<Ascension> ascensions = parseAscensions(node.get("ascension"));
            List<Eidolon> eidolons = parseEidolons(node.get("eidolons"));
            Skill skills = parseSkills(node.get("skills"));
            Trace traces = parseTraces(node.get("traces"));

            String iconUrl = getText(node, "icon");
            String splashUrl = getText(node, "splash");
            String miniIconUrl = getText(node, "mini_icon");

            CharBuild build = characterBuildService.getBuildForCharacter(name);

            Character character = Character.builder()
                    .id(name.hashCode())
                    .name(name)
                    .rarity(node.get("rarity").asInt())
                    .element(Element.valueOf(elementRaw.toUpperCase()))
                    .path(Path.valueOf(
                            pathRaw.replace("The ", "").toUpperCase()
                    ))
                    .icon(iconUrl)
                    .splash(splashUrl)
                    .miniIcon(miniIconUrl)
                    .ascension(ascensions)
                    .eidolons(eidolons)
                    .skills(skills)
                    .traces(traces)
                    .build(build)
                    .build();

            characters.put(character.getId(), character);
        }
    }

    private void loadLightcones(JsonNode lightconesNode) throws IOException {
        if (lightconesNode == null) return;

        for (Iterator<String> it = lightconesNode.propertyNames().iterator(); it.hasNext(); ) {
            String name = it.next();
            JsonNode node = lightconesNode.get(name);

            if (!node.has("rarity")) continue;

            String pathRaw = getText(node, "path");

            if (pathRaw == null) continue;

            List<LightconeAscension> ascensions = parseLightconeAscensions(node.get("ascension"));
            LightconeAbility ability = parseLightconeAbility(node.get("ability"));

            Lightcone lightcone = Lightcone.builder()
                    .id(name.hashCode())
                    .name(name)
                    .rarity(node.get("rarity").asInt())
                    .path(Path.valueOf(
                            pathRaw.replace("The ", "").toUpperCase()
                    ))
                    .desc(getText(node, "desc"))
                    .icon(getText(node, "icon"))
                    .image(getText(node, "image"))
                    .miniIcon(getText(node, "mini_icon"))
                    .ascensions(ascensions)
                    .ability(ability)
                    .build();

            lightcones.put(lightcone.getId(), lightcone);
        }
    }

    private RelicSet parseRelicSet(String name, JsonNode node) {
        try {
            Map<String, Piece> pieces = parsePieces(node.get("pieces"));
            List<String> desc = parseDescription(node.get("desc"));
            List<List<Modifier>> modifiers = parseSetModifiers(node.get("modifiers"));

            return RelicSet.builder()
                    .name(name)
                    .pieces(pieces)
                    .desc(desc)
                    .modifiers(modifiers)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing relic set: {}", name, e);
            return null;
        }
    }

    private Map<String, Piece> parsePieces(JsonNode piecesNode) {
        Map<String, Piece> pieces = new HashMap<>();
        if (piecesNode == null || !piecesNode.isObject()) {
            return pieces;
        }

        Map<String, Object> piecesMap = mapper.convertValue(piecesNode, Map.class);

        for (Map.Entry<String, Object> entry : piecesMap.entrySet()) {
            String pieceType = entry.getKey();

            JsonNode pieceNode = mapper.valueToTree(entry.getValue());

            Piece piece = Piece.builder()
                    .name(getText(pieceNode, "name"))
                    .icon(getText(pieceNode, "icon"))
                    .build();

            pieces.put(pieceType, piece);
        }

        return pieces;
    }

    private List<String> parseDescription(JsonNode descNode) {
        List<String> desc = new ArrayList<>();
        if (descNode == null || !descNode.isArray()) {
            return desc;
        }

        for (JsonNode descItem : descNode) {
            desc.add(descItem.asText());
        }

        return desc;
    }

    private List<List<Modifier>> parseSetModifiers(JsonNode modifiersNode) {
        List<List<Modifier>> allModifiers = new ArrayList<>();
        if (modifiersNode == null || !modifiersNode.isArray()) {
            return allModifiers;
        }

        for (JsonNode modifierArray : modifiersNode) {
            List<Modifier> modifiers = new ArrayList<>();
            if (modifierArray.isArray()) {
                for (JsonNode modifierNode : modifierArray) {
                    Modifier modifier = Modifier.builder()
                            .type(modifierNode.get("type").asText())
                            .value(modifierNode.get("value").asDouble())
                            .build();
                    modifiers.add(modifier);
                }
            }
            allModifiers.add(modifiers);
        }

        return allModifiers;
    }

    private List<LightconeAscension> parseLightconeAscensions(JsonNode ascensionsNode) {
        List<LightconeAscension> ascensions = new ArrayList<>();
        if (ascensionsNode == null || !ascensionsNode.isArray()) {
            return ascensions;
        }

        for (JsonNode ascNode : ascensionsNode) {
            LightconeAscension asc = LightconeAscension.builder()
                    .hp(parseStatValue(ascNode.get("hp")))
                    .atk(parseStatValue(ascNode.get("atk")))
                    .def(parseStatValue(ascNode.get("def")))
                    .build();
            ascensions.add(asc);
        }
        return ascensions;
    }

    private LightconeAbility parseLightconeAbility(JsonNode abilityNode) {
        if (abilityNode == null) return null;

        List<List<String>> params = new ArrayList<>();
        JsonNode paramsNode = abilityNode.get("params");
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramArray : paramsNode) {
                List<String> paramList = new ArrayList<>();
                if (paramArray.isArray()) {
                    for (JsonNode param : paramArray) {
                        paramList.add(param.asText());
                    }
                }
                params.add(paramList);
            }
        }
        return LightconeAbility.builder()
                .name(getText(abilityNode, "name"))
                .desc(getText(abilityNode, "desc"))
                .params(params)
                .build();
    }

    public Collection<Lightcone> getAllLightcones() {
        return lightcones.values();
    }

    public Lightcone getLightconeById(int id) {
        return lightcones.get(id);
    }

    public Lightcone getLightconeByName(String name) {
        return lightcones.get(name.hashCode());
    }

    public Collection<Character> getAllCharacters() {
        return characters.values();
    }

    public Character getById(int id) {
        return characters.get(id);
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asString() : null;
    }

    public Collection<RelicSet> getAllRelicSets() {
        return relicSets.values();
    }

    public RelicSet getRelicSetByName(String name) {
        return relicSets.get(name);
    }

    /*private String buildIconPath(String characterName) {
        String safe = characterName.replaceAll("[^a-zA-Z0-9^]", "");
        return "icons/mini/" + safe + ".png";
    }*/

    private List<Ascension> parseAscensions(JsonNode ascensionsNode) {
        List<Ascension> ascensions = new ArrayList<>();
        if (ascensionsNode == null || !ascensionsNode.isArray()) {
            return ascensions;
        }

        for (JsonNode ascNode : ascensionsNode) {
            Ascension asc = Ascension.builder()
                    .hp(parseStatValue(ascNode.get("hp")))
                    .atk(parseStatValue(ascNode.get("atk")))
                    .def(parseStatValue(ascNode.get("def")))
                    .spd(parseStatValue(ascNode.get("spd")))
                    .taunt(parseStatValue(ascNode.get("taunt")))
                    .critRate(parseStatValue(ascNode.get("crit_rate")))
                    .critDmg(parseStatValue(ascNode.get("crit_dmg")))
                    .build();
            ascensions.add(asc);
        }
        return ascensions;
    }

    private StatValue parseStatValue(JsonNode statNode) {
        if (statNode == null) return null;

        return StatValue.builder()
                .base(statNode.get("base").asDouble())
                .step(statNode.get("step").asDouble())
                .build();
    }

    private List<Eidolon> parseEidolons(JsonNode eidolonsNode) {
        List<Eidolon> eidolons = new ArrayList<>();
        if (eidolonsNode == null || !eidolonsNode.isArray()) {
            return eidolons;
        }

        for (JsonNode eidolonNode : eidolonsNode) {
            Eidolon eidolon = Eidolon.builder()
                    .name(eidolonNode.get("name").asString())
                    .desc(eidolonNode.get("desc").asString())
                    .build();

            JsonNode levelUpSkillsNode = eidolonNode.get("level_up_skills");
            if (levelUpSkillsNode != null) {
                Map levelUpSkills = mapper.convertValue(levelUpSkillsNode, Map.class);
                eidolon.setLevelUpSkills(levelUpSkills);
            }

            eidolons.add(eidolon);
        }
        return eidolons;
    }

    private Skill parseSkills(JsonNode skillsNode) {
        if (skillsNode == null) return null;

        Skill skill = Skill.builder()
                .basic(parseBasicSkill(skillsNode.get("basic")))
                .skill(parseActiveSkill(skillsNode.get("skill")))
                .ult(parseUltimateSkill(skillsNode.get("ult")))
                .talent(parseTalentSkill(skillsNode.get("talent")))
                .build();

        return skill;
    }

    private BasicSkill parseBasicSkill(JsonNode basicNode) {
        if (basicNode == null) return null;

        List<List<String>> params = new ArrayList<>();
        JsonNode paramsNode = basicNode.get("params");
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramArray : paramsNode) {
                List<String> paramList = new ArrayList<>();
                if (paramArray.isArray()) {
                    for (JsonNode param : paramArray) {
                        paramList.add(param.asString());
                    }
                }
                params.add(paramList);
            }
        }

        return BasicSkill.builder()
                .name(basicNode.get("name").asString())
                .maxLevel(basicNode.get("max_level").asInt())
                .desc(basicNode.get("desc").asString())
                .params(params)
                .build();
    }

    private ActiveSkill parseActiveSkill(JsonNode skillNode) {
        if (skillNode == null) return null;

        List<List<String>> params = new ArrayList<>();
        JsonNode paramsNode = skillNode.get("params");
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramArray : paramsNode) {
                List<String> paramList = new ArrayList<>();
                if (paramArray.isArray()) {
                    for (JsonNode param : paramArray) {
                        paramList.add(param.asString());
                    }
                }
                params.add(paramList);
            }
        }

        return ActiveSkill.builder()
                .name(skillNode.get("name").asString())
                .maxLevel(skillNode.get("max_level").asInt())
                .desc(skillNode.get("desc").asString())
                .params(params)
                .build();
    }

    private UltimateSkill parseUltimateSkill(JsonNode ultNode) {
        if (ultNode == null) return null;

        List<List<String>> params = new ArrayList<>();
        JsonNode paramsNode = ultNode.get("params");
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramArray : paramsNode) {
                List<String> paramList = new ArrayList<>();
                if (paramArray.isArray()) {
                    for (JsonNode param : paramArray) {
                        paramList.add(param.asString());
                    }
                }
                params.add(paramList);
            }
        }

        return UltimateSkill.builder()
                .name(ultNode.get("name").asString())
                .maxLevel(ultNode.get("max_level").asInt())
                .desc(ultNode.get("desc").asString())
                .params(params)
                .build();
    }

    private TalentSkill parseTalentSkill(JsonNode talentNode) {
        if (talentNode == null) return null;

        List<List<String>> params = new ArrayList<>();
        JsonNode paramsNode = talentNode.get("params");
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramArray : paramsNode) {
                List<String> paramList = new ArrayList<>();
                if (paramArray.isArray()) {
                    for (JsonNode param : paramArray) {
                        paramList.add(param.asString());
                    }
                }
                params.add(paramList);
            }
        }

        return TalentSkill.builder()
                .name(talentNode.get("name").asString())
                .maxLevel(talentNode.get("max_level").asInt())
                .desc(talentNode.get("desc").asString())
                .params(params)
                .build();
    }

    private Trace parseTraces(JsonNode tracesNode) {
        if (tracesNode == null) return null;

        Trace trace = Trace.builder()
                .technique(parseTechniqueTrace(tracesNode.get("technique")))
                .abilities(parseAbilities(tracesNode))
                .stats(parseStatTraces(tracesNode))
                .build();

        return trace;
    }

    private TechniqueTrace parseTechniqueTrace(JsonNode techniqueNode) {
        if (techniqueNode == null) return null;

        return TechniqueTrace.builder()
                .name(techniqueNode.get("name").asString())
                .desc(techniqueNode.get("desc").asString())
                .build();
    }

    private Map<String, AbilityTrace> parseAbilities(JsonNode tracesNode) {
        Map<String, AbilityTrace> abilities = new HashMap<>();

        for (int i = 1; i <= 3; i++) {
            String key = "ability_" + i;
            JsonNode abilityNode = tracesNode.get(key);
            if (abilityNode != null) {
                AbilityTrace ability = AbilityTrace.builder()
                        .name(abilityNode.get("name").asString())
                        .desc(abilityNode.get("desc").asString())
                        .build();
                abilities.put(key, ability);
            }
        }

        return abilities;
    }

    private Map<String, StatTrace> parseStatTraces(JsonNode tracesNode) {
        Map<String, StatTrace> stats = new HashMap<>();

        for (int i = 1; i <= 10; i++) {
            String key = "stat_" + i;
            JsonNode statNode = tracesNode.get(key);
            if (statNode != null) {
                StatTrace stat = StatTrace.builder()
                        .name(statNode.get("name").asString())
                        .desc(statNode.get("desc").asString())
                        .modifiers(parseModifiers(statNode.get("modifiers")))
                        .build();
                stats.put(key, stat);
            }
        }

        return stats;
    }

    private List<Modifier> parseModifiers(JsonNode modifiersNode) {
        List<Modifier> modifiers = new ArrayList<>();
        if (modifiersNode == null || !modifiersNode.isArray()) {
            return modifiers;
        }

        for (JsonNode modifierNode : modifiersNode) {
            Modifier modifier = Modifier.builder()
                    .type(modifierNode.get("type").asString())
                    .value(modifierNode.get("value").asDouble())
                    .build();
            modifiers.add(modifier);
        }

        return modifiers;
    }
}
