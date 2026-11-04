"""Refreshes data-pipeline/character_lore.json (each character's profile texts from the game).

For every character it keeps what the in-game Character Archive shows:
  - description: the "Character Details" text
  - stories: the Character Stories (plus the extra chapters some characters get)
  - voice_actors: English, Japanese, Chinese and Korean voice actors, when listed

The texts come from the same game data dump as the item descriptions, so both scripts share one
download (update_item_descriptions.fetch). update_data.py merges the result into game_data.json.

Usage (from the repository root):

    python data-pipeline/update_character_lore.py
"""

import collections
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from update_item_descriptions import CACHE, ROOT, fetch  # noqa: E402

OUTPUT = os.path.join(ROOT, "data-pipeline", "character_lore.json")
GAME_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "game_data.json")

# Versions that don't have an archive entry of their own reuse another one:
#   the Trailblazer's Path variants share the Trailblazer's (8001 male, 8002 female), and
#   March 7th's Xianzhou version (1224) is filed under March 7th's extra entries 21 and 22.
SPLIT_ENTRIES = {1224: (1001, {21, 22})}
OWN_ENTRIES_EXCLUDE = {1001: {21, 22}}
VOICE_LANGUAGES = [("CV_EN", "English"), ("CV_JP", "Japanese"), ("CV_CN", "Chinese"), ("CV_KR", "Korean")]


def tidy(text, female):
    """Game rich text -> plain text that only keeps <i> and <b> (the frontend renders those two).

    Gendered pieces like {F#her}{M#his} pick the Trailblazer's form, {NICKNAME} becomes
    "Trailblazer", and ruby annotations {RUBY_B#note}word{RUBY_E#} become "word (note)".
    """
    if not text:
        return None
    text = re.sub(r"\{F#([^}]*)\}", lambda m: m.group(1) if female else "", text)
    text = re.sub(r"\{M#([^}]*)\}", lambda m: "" if female else m.group(1), text)
    text = text.replace("{NICKNAME}", "Trailblazer")
    text = re.sub(r"\{RUBY_B#([^}]*)\}(.*?)\{RUBY_E#\}", r"\2 (\1)", text)
    text = re.sub(r"\{[A-Z_]+#?[^}]*\}", "", text)
    text = re.sub(r"<(?!/?[ib]>)[^>]*>", "", text)  # drop every tag except <i>, </i>, <b>, </b>
    text = text.replace("\\n", "\n").replace("\r", "")
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text or None


def main():
    if "--skip-fetch" not in sys.argv:
        fetch()

    def load(name):
        with open(os.path.join(CACHE, *name.split("/")), encoding="utf-8") as f:
            return json.load(f)

    text_map = load("TextMap/TextMapEN.json")

    def text(ref):
        return text_map.get(str(ref.get("Hash"))) if isinstance(ref, dict) else None

    stories = collections.defaultdict(list)
    for entry in load("ExcelOutput/StoryAtlas.json"):
        stories[entry["AvatarID"]].append(entry)
    atlas = {a["AvatarID"]: a for a in load("ExcelOutput/AvatarAtlas.json")}

    with open(GAME_DATA, encoding="utf-8") as f:
        characters = json.load(f)["characters"]

    out, missing = {}, []
    for name, char in characters.items():
        cid = int(char["id"])
        female = name.endswith("#F")
        if name.startswith("Trailblazer"):
            source, wanted = (8002 if female else 8001), None
        elif cid in SPLIT_ENTRIES:
            source, wanted = SPLIT_ENTRIES[cid]
        else:
            source, wanted = cid, None
        entries = sorted(stories.get(source, []), key=lambda e: e["StoryID"])
        if wanted is not None:
            entries = [e for e in entries if e["StoryID"] in wanted]
        else:
            entries = [e for e in entries if e["StoryID"] not in OWN_ENTRIES_EXCLUDE.get(source, set())]

        texts = [t for t in (tidy(text(e.get("Story")), female) for e in entries) if t]
        if not texts:
            missing.append(name)
            continue

        voice = {}
        if source in atlas:
            for key, label in VOICE_LANGUAGES:
                actor = (text(atlas[source].get(key)) or "").strip()
                if actor and actor.upper() not in {"N/A", "TBA"}:
                    voice[label] = actor

        lore = {"description": texts[0], "stories": texts[1:]}
        if voice:
            lore["voice_actors"] = voice
        out[str(cid)] = lore

    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1, sort_keys=True)
    print(f"Lore for {len(out)} of {len(characters)} characters -> {os.path.relpath(OUTPUT, ROOT)}")
    if missing:
        print("No archive texts yet for: " + ", ".join(missing))


if __name__ == "__main__":
    main()
