"""Builds src/main/resources/data/planner_data.json: everything the material planner needs.

  - characters: ascension costs per phase, skill level-up costs and the one-off trace nodes
  - light_cones: ascension costs per phase and EXP table (by light cone name, like the rest of the wiki)
  - exp: EXP needed per level (characters and each light cone rarity) and the EXP items
  - items: name, icon and rarity of every material used above

Costs come from StarRailRes (same cache as update_data.py); the EXP tables come from the game
data dump used for the item and lore texts (update_item_descriptions.fetch).

Usage (from the repository root, after update_data.py):

    python data-pipeline/update_planner_data.py
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from update_item_descriptions import CACHE as DUMP, ROOT, fetch  # noqa: E402

RES = os.path.join(ROOT, "data-pipeline", ".cache", "StarRailRes", "index_new", "en")
ICON_BASE = "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/"
GAME_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "game_data.json")
OUTPUT = os.path.join(ROOT, "src", "main", "resources", "data", "planner_data.json")

# Skill tree anchors that level a skill (the rest are one-off trace nodes)
SKILL_ANCHORS = {"Point01": "basic", "Point02": "skill", "Point03": "ult", "Point04": "talent",
                 "Point19": "memo_skill", "Point20": "memo_talent", "Point22": "elation_skill"}
LEVEL_CAPS = [20, 30, 40, 50, 60, 70, 80]
# The game shows the Credit cost of character EXP items when used, but it isn't in the data files:
# 10% of the EXP is the commonly used figure. Light cone EXP items carry their own cost.
CHARACTER_CREDITS_PER_EXP = 0.1


def load(*parts):
    with open(os.path.join(*parts), encoding="utf-8") as f:
        return json.load(f)


def mats(entries):
    return [[m["id"], m["num"]] for m in entries or [] if m.get("num")]


def main():
    if "--skip-fetch" not in sys.argv:
        fetch()

    promotions = load(RES, "character_promotions.json")
    trees = load(RES, "character_skill_trees.json")
    lc_promotions = load(RES, "light_cone_promotions.json")
    lc_info = load(RES, "light_cones.json")
    res_items = load(RES, "items.json")
    game = load(GAME_DATA)

    used = set()

    def track(rows):
        for row in rows:
            used.add(row[0])
        return rows

    characters = {}
    for name, char in game["characters"].items():
        cid = str(char["id"])
        promo = promotions.get(cid)
        if not promo:
            continue
        skills, traces = {}, []
        for node_id, node in trees.items():
            if not node_id.startswith(cid) or len(node_id) != len(cid) + 3:
                continue
            key = SKILL_ANCHORS.get(node["anchor"])
            if key and node["max_level"] > 1:
                # levels[i] is the cost of reaching level i + 1 (level 1 is free)
                skills[key] = [track(mats(level["materials"])) for level in node["levels"]]
            elif node["levels"] and node["levels"][0]["materials"]:
                traces.append(track(mats(node["levels"][0]["materials"])))
        characters[cid] = {
            "ascension": [track(mats(phase)) for phase in promo["materials"][:6]],
            "skills": skills,
            "traces": traces,
        }

    lc_by_name = {v["name"]: k for k, v in lc_info.items()}
    light_cones = {}
    exp_type = {str(e["EquipmentID"]): e["ExpType"] for e in load(DUMP, "ExcelOutput", "EquipmentConfig.json")}
    for name in game["light_cones"]:
        gid = lc_by_name.get(name)
        if not gid or gid not in lc_promotions:
            continue
        light_cones[name] = {
            "rarity": lc_info[gid]["rarity"],
            "exp_type": exp_type.get(gid, {3: 1, 4: 2, 5: 3}[lc_info[gid]["rarity"]]),
            "ascension": [track(mats(phase)) for phase in lc_promotions[gid]["materials"][:6]],
        }

    def exp_table(rows, type_key, type_value):
        table = {r["Level"]: r.get("Exp", 0) for r in rows if r[type_key] == type_value}
        return [table.get(level, 0) for level in range(1, 80)]  # EXP to go from level L to L + 1

    avatar_group = load(DUMP, "ExcelOutput", "AvatarConfig.json")[0]["ExpGroup"]
    exp = {
        "character": exp_table(load(DUMP, "ExcelOutput", "ExpType.json"), "TypeID", avatar_group),
        "light_cone": {str(t): exp_table(load(DUMP, "ExcelOutput", "EquipmentExpType.json"), "ExpType", t) for t in (1, 2, 3)},
        "character_items": sorted(([str(i["ItemID"]), i["Exp"]] for i in load(DUMP, "ExcelOutput", "AvatarExpItemConfig.json")), key=lambda x: -x[1]),
        "light_cone_items": sorted(([str(i["ItemID"]), i["ExpProvide"], i["CoinCost"]] for i in load(DUMP, "ExcelOutput", "EquipmentExpItemConfig.json")), key=lambda x: -x[1]),
        "character_credits_per_exp": CHARACTER_CREDITS_PER_EXP,
        "level_caps": LEVEL_CAPS,
    }
    used.update(i[0] for i in exp["character_items"] + exp["light_cone_items"])
    used.add("2")

    items = {}
    for item_id in sorted(used, key=int):
        info = res_items.get(item_id, {})
        items[item_id] = {"name": info.get("name", f"Item {item_id}"), "rarity": info.get("rarity", 1),
                          "icon": ICON_BASE + info["icon"] if info.get("icon") else None}

    out = {"version": game.get("version"), "characters": characters, "light_cones": light_cones, "exp": exp, "items": items}
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
    print(f"Planner data: {len(characters)} characters, {len(light_cones)} light cones, {len(items)} materials "
          f"-> {os.path.relpath(OUTPUT, ROOT)} ({os.path.getsize(OUTPUT) // 1024} KB)")


if __name__ == "__main__":
    main()
