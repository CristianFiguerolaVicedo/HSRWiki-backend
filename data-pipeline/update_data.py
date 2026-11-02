"""Regenerates the game data used by the HSRWiki backend.

Usage (from the repository root):

    python data-pipeline/update_data.py

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
REPO = "https://github.com/Mar-7th/StarRailRes.git"

# Item sub types that are just collectibles/lore/minigame tokens; they made the item list huge (~4000)
SKIPPED_ITEM_TYPES = {"Mission"}
SKIPPED_ITEM_SUB_TYPES = {
    "Book", "MusicAlbum", "TravelBrochurePaster", "RelicSetShowOnly", "PlanetFesItem",
    "ChessRogueDiceSurface", "DiceCombatDice", "DiceCombatAvatar", "ElfRestaurantItem",
    "MuseumStuff", "MuseumExhibit", "Mission",
}


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


def get_items(res_path):
    with open(os.path.join(res_path, "index_new", "en", "items.json"), encoding="utf-8") as f:
        raw = json.load(f)
    base = "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/"
    items = {}
    for item in raw.values():
        if item.get("type") in SKIPPED_ITEM_TYPES or item.get("sub_type") in SKIPPED_ITEM_SUB_TYPES:
            continue
        name = item["name"]
        if not name or name in items:
            continue
        items[name] = {
            "id": item["id"],
            "rarity": item.get("rarity", 1),
            "type": item.get("type"),
            "sub_type": item.get("sub_type"),
            "icon": base + item["icon"] if item.get("icon") else None,
            "come_from": item.get("come_from", []),
        }
    return items


def main():
    if "--skip-fetch" not in sys.argv:
        fetch_star_rail_res()
    os.environ.setdefault("STAR_RAIL_RES_PATH", CACHE)
    sys.path.insert(0, os.path.join(ROOT, "data-pipeline"))
    from hsrdata.game_data_verbose import get_game_data_verbose  # reads STAR_RAIL_RES_PATH on import

    data = get_game_data_verbose(include_icons=True)
    data["items"] = get_items(os.environ["STAR_RAIL_RES_PATH"])

    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(
        f"Game version {data['version']}: {len(data['characters'])} characters, "
        f"{len(data['light_cones'])} light cones, {len(data['relic_sets'])} relic sets, "
        f"{len(data['items'])} items -> {os.path.relpath(OUTPUT, ROOT)}"
    )


if __name__ == "__main__":
    main()
