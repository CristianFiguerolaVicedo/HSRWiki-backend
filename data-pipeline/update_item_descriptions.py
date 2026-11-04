"""Refreshes data-pipeline/item_descriptions.json (item descriptions and lore text).

StarRailRes doesn't include item descriptions, so they come from a public dump of the game's
data tables (DimbreathBot/TurnBasedGameData, kept up to date with each patch). Only the needed files are downloaded (~55 MB, mostly
the English text map), and the result is a small file that update_data.py merges in.

Usage (from the repository root):

    python data-pipeline/update_item_descriptions.py

Items released after the dump's version simply have no description until it is updated.
"""

import json
import os
import re
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "data-pipeline", ".cache", "TurnBasedGameData")
OUTPUT = os.path.join(ROOT, "data-pipeline", "item_descriptions.json")
REPO = "https://github.com/DimbreathBot/TurnBasedGameData.git"
CONFIGS = ["ItemConfig", "ItemConfigBook", "ItemConfigDisk", "ItemConfigEquipment"]


def git(*args, cwd=None):
    subprocess.run(["git", *args], cwd=cwd, check=True)


# Every file the dump-based scripts (this one, update_character_lore.py, update_planner_data.py) read, so they share one cache
FILES = [f"/ExcelOutput/{c}.json" for c in CONFIGS] + [
    "/ExcelOutput/AvatarConfig.json", "/ExcelOutput/AvatarAtlas.json", "/ExcelOutput/StoryAtlas.json",
    # EXP tables for update_planner_data.py
    "/ExcelOutput/ExpType.json", "/ExcelOutput/EquipmentExpType.json", "/ExcelOutput/AvatarExpItemConfig.json",
    "/ExcelOutput/EquipmentExpItemConfig.json", "/ExcelOutput/EquipmentConfig.json",
    "/TextMap/TextMapEN.json",
]


def cached_remote():
    try:
        out = subprocess.run(["git", "remote", "get-url", "origin"], cwd=CACHE, capture_output=True, text=True)
        return out.stdout.strip()
    except OSError:
        return None


def fetch():
    paths = FILES
    if os.path.isdir(os.path.join(CACHE, ".git")) and cached_remote() != REPO:
        # The cache came from an older mirror: start over with the current one
        import shutil
        shutil.rmtree(CACHE)
    if not os.path.isdir(os.path.join(CACHE, ".git")):
        os.makedirs(os.path.dirname(CACHE), exist_ok=True)
        git("clone", "--depth", "1", "--filter=blob:none", "--no-checkout", REPO, CACHE)
        git("sparse-checkout", "set", "--no-cone", *paths, cwd=CACHE)
        git("checkout", cwd=CACHE)
    else:
        git("sparse-checkout", "set", "--no-cone", *paths, cwd=CACHE)
        git("pull", "--depth", "1", cwd=CACHE)


def clean(text):
    """Strip the game's rich-text markup (<i>, <color=...>, <unbreak>) and tidy whitespace."""
    if not text:
        return None
    text = re.sub(r"</?[a-zA-Z]+(=[^>]*)?>", "", text)
    text = text.replace("\\n", "\n").strip()
    return re.sub(r"\n{3,}", "\n\n", text) or None


def main():
    fetch()
    with open(os.path.join(CACHE, "TextMap", "TextMapEN.json"), encoding="utf-8") as f:
        text_map = json.load(f)

    def text(ref):
        return text_map.get(str(ref.get("Hash"))) if isinstance(ref, dict) else None

    out = {}
    for config in CONFIGS:
        with open(os.path.join(CACHE, "ExcelOutput", f"{config}.json"), encoding="utf-8") as f:
            for item in json.load(f):
                desc = clean(text(item.get("ItemDesc")))
                lore = clean(text(item.get("ItemBGDesc")))
                if desc == lore:
                    lore = None
                if desc or lore:
                    out[str(item["ID"])] = {k: v for k, v in (("desc", desc), ("lore", lore)) if v}

    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    print(f"{len(out)} item descriptions -> {os.path.relpath(OUTPUT, ROOT)}")


if __name__ == "__main__":
    main()
