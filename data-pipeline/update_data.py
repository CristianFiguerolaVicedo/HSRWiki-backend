"""Regenerates the game data used by the HSRWiki backend.

Usage (from the repository root):

    python data-pipeline/update_data.py

Item descriptions come from data-pipeline/item_descriptions.json; refresh that file with
update_item_descriptions.py when a newer game data dump is available.

It downloads only the English index files of Mar-7th/StarRailRes (no images, a few MB),
runs the extractor and writes src/main/resources/data/game_data.json.
Run it again after every Honkai: Star Rail patch, once StarRailRes has been updated.
"""

import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "data-pipeline", ".cache", "StarRailRes")
OUTPUT = os.path.join(ROOT, "src", "main", "resources", "data", "game_data.json")
SHOWCASE_LOOKUP = os.path.join(ROOT, "src", "main", "resources", "data", "showcase_lookup.json")
REPO = "https://github.com/Mar-7th/StarRailRes.git"

DESCRIPTIONS = os.path.join(ROOT, "data-pipeline", "item_descriptions.json")

# Item types/sub types left out of the wiki: lore collectibles, one-off event and minigame
# tokens, pets and display-only entries. They made the list huge and say nothing useful.
SKIPPED_ITEM_TYPES = {"Mission", "Pet", "Display"}
SKIPPED_ITEM_SUB_TYPES = {
    "Book", "MusicAlbum", "TravelBrochurePaster", "RelicSetShowOnly", "PlanetFesItem",
    "ChessRogueDiceSurface", "DiceCombatDice", "DiceCombatAvatar", "ElfRestaurantItem",
    "MuseumStuff", "MuseumExhibit", "Mission",
    "AetherSkill", "AetherSpirit", "FightFestSkill", "HipplenOutfit", "IdleLiveItem",
    "MatchThreeV2", "PixAirMaterial", "HeadIconFrame", "PlatformBoundGift",
}

# Readable names for the game's internal item sub types (shown in filters and item pages)
CATEGORY_NAMES = {
    "Virtual": "Currency",
    "Material": "Material",
    "TracePath": "Trace Material",
    "CommonMonsterDrop": "Enemy Drop",
    "WeeklyMonsterDrop": "Weekly Boss Material",
    "AvatarRank": "Ascension Material",
    "ComposeMaterial": "Synthesis Material",
    "Eidolon": "Eidolon",
    "AvatarExp": "Character EXP",
    "EquipmentExp": "Light Cone EXP",
    "RelicExp": "Relic EXP",
    "Food": "Consumable",
    "Formula": "Recipe",
    "Gift": "Gift Box",
    "ForceOpitonalGift": "Selector Box",
    "FindChest": "Treasure Finder",
    "PlayerOutfit": "Outfit",
    "PamSkin": "Pom-Pom Outfit",
    "PhoneTheme": "Phone Theme",
    "PhoneCase": "Phone Case",
    "ChatBubble": "Chat Bubble",
    "PersonalCard": "Profile Card",
    "RogueMedal": "Medal",
}

# Materials used by (almost) everyone, like Credit; listing every character for them is noise
USED_BY_LIMIT = 40

def git(*args, cwd=None):
    subprocess.run(["git", *args], cwd=cwd, check=True)


def fetch_star_rail_res():
    """Sparse, blob-less checkout of just the files the extractor needs."""
    if not os.path.isdir(os.path.join(CACHE, ".git")):
        os.makedirs(os.path.dirname(CACHE), exist_ok=True)
        git("clone", "--depth", "1", "--filter=blob:none", "--no-checkout", REPO, CACHE)
        git("sparse-checkout", "set", "--no-cone", "/index_new/en/", "/info.json", cwd=CACHE)
        git("checkout", cwd=CACHE)
    else:
        git("pull", "--depth", "1", cwd=CACHE)


def _uses(res_path, characters, light_cones):
    """Which characters and light cones need each material (ascension + trace level-ups)."""
    en = os.path.join(res_path, "index_new", "en")
    load = lambda name: json.load(open(os.path.join(en, name), encoding="utf-8"))
    char_promotions, trees, lc_promotions = load("character_promotions.json"), load("character_skill_trees.json"), load("light_cone_promotions.json")

    char_name = {c["id"]: name for name, c in characters.items()}
    raw_chars = load("characters.json")
    lc_name = {lid: lc["name"] for lid, lc in load("light_cones.json").items() if lc["name"] in light_cones}

    used = {}

    def add(item_id, kind, name):
        used.setdefault(item_id, {"characters": set(), "light_cones": set()})[kind].add(name)

    for cid, promo in char_promotions.items():
        name = char_name.get(cid)
        if not name:
            continue
        for step in promo["materials"]:
            for m in step:
                add(m["id"], "characters", name)
        for tree_id in raw_chars.get(cid, {}).get("skill_trees", []):
            for level in trees.get(tree_id, {}).get("levels", []):
                for m in level.get("materials", []):
                    add(m["id"], "characters", name)
    for lid, promo in lc_promotions.items():
        name = lc_name.get(lid)
        if not name:
            continue
        for step in promo["materials"]:
            for m in step:
                add(m["id"], "light_cones", name)
    return used


def _item_name(name):
    if "{TEXTJOIN" in name:
        return None
    return name.replace("{NICKNAME}", "Trailblazer").replace("{M#", "").replace("{F#", "")


def get_items(res_path, characters, light_cones):
    with open(os.path.join(res_path, "index_new", "en", "items.json"), encoding="utf-8") as f:
        raw = json.load(f)
    descriptions = {}
    if os.path.exists(DESCRIPTIONS):
        with open(DESCRIPTIONS, encoding="utf-8") as f:
            descriptions = json.load(f)
    used = _uses(res_path, characters, light_cones)

    base = "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/"
    items = {}
    for item in raw.values():
        if item.get("type") in SKIPPED_ITEM_TYPES or item.get("sub_type") in SKIPPED_ITEM_SUB_TYPES:
            continue
        name = _item_name(item["name"] or "")
        if not name or name in items:
            continue
        info = descriptions.get(item["id"], {})
        uses = used.get(item["id"], {"characters": set(), "light_cones": set()})
        too_common = len(uses["characters"]) + len(uses["light_cones"]) > USED_BY_LIMIT
        items[name] = {
            "id": item["id"],
            "rarity": item.get("rarity", 1),
            "type": item.get("type"),
            "sub_type": item.get("sub_type"),
            "category": CATEGORY_NAMES.get(item.get("sub_type"), item.get("sub_type")),
            "icon": base + item["icon"] if item.get("icon") else None,
            "come_from": item.get("come_from", []),
            "desc": info.get("desc"),
            "lore": info.get("lore"),
            "used_by": {
                "characters": [] if too_common else sorted(uses["characters"]),
                "light_cones": [] if too_common else sorted(uses["light_cones"]),
                "everyone": too_common,
            },
        }
    return items

def get_showcase_lookup(res_path):
    """IDs used by Enka.Network's showcase API -> names and stat formulas (see ShowcaseService)."""
    en = os.path.join(res_path, "index_new", "en")
    load = lambda name: json.load(open(os.path.join(en, name), encoding="utf-8"))
    base = "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/"
    slots = {"HEAD": "Head", "HAND": "Hands", "BODY": "Body", "FOOT": "Feet", "NECK": "Planar Sphere", "OBJECT": "Link Rope"}

    set_names = {sid: s["name"] for sid, s in load("relic_sets.json").items()}
    relics = {
        rid: {
            "set": set_names.get(r["set_id"]),
            "slot": slots.get(r["type"], r["type"]),
            "rarity": r["rarity"],
            "main_group": r["main_affix_id"],
            "sub_group": r["sub_affix_id"],
            "icon": base + r["icon"] if r.get("icon") else None,
        }
        for rid, r in load("relics.json").items()
    }
    affixes = lambda name: {
        gid: {aid: {"property": a["property"], "base": a["base"], "step": a["step"]} for aid, a in g["affixes"].items()}
        for gid, g in load(name).items()
    }
    properties = {k: {"name": v["name"], "percent": bool(v.get("percent"))} for k, v in load("properties.json").items()}
    return {
        "light_cones": {lid: lc["name"] for lid, lc in load("light_cones.json").items()},
        "relics": relics,
        "main_affixes": affixes("relic_main_affixes.json"),
        "sub_affixes": affixes("relic_sub_affixes.json"),
        "properties": properties,
    }


def main():
    if "--skip-fetch" not in sys.argv:
        fetch_star_rail_res()
    os.environ.setdefault("STAR_RAIL_RES_PATH", CACHE)
    sys.path.insert(0, os.path.join(ROOT, "data-pipeline"))
    from hsrdata.game_data_verbose import get_game_data_verbose  # reads STAR_RAIL_RES_PATH on import

    data = get_game_data_verbose(include_icons=True)
    data["items"] = get_items(os.environ["STAR_RAIL_RES_PATH"], data["characters"], data["light_cones"])

    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    with open(SHOWCASE_LOOKUP, "w", encoding="utf-8") as f:
        json.dump(get_showcase_lookup(os.environ["STAR_RAIL_RES_PATH"]), f, ensure_ascii=False, separators=(",", ":"))

    print(
        f"Game version {data['version']}: {len(data['characters'])} characters, "
        f"{len(data['light_cones'])} light cones, {len(data['relic_sets'])} relic sets, "
        f"{len(data['items'])} items -> {os.path.relpath(OUTPUT, ROOT)}"
    )


if __name__ == "__main__":
    main()
