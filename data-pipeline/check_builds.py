"""Checks character_builds.json against game_data.json and lists characters without a build.

    python data-pipeline/check_builds.py

Every light cone, relic set and planar set named in a build must exist in the game data with the
same spelling, otherwise the frontend can't show its image or link. Team members must be a
character name, an alternative written as "A / B", "Trailblazer (Path)" or a generic slot
(anything starting with "Any", plus a few role words). Exits with an error if something is wrong.

It also checks endgame_teams.json (recommended endgame teams) with the same rules, and that each
of its phases exists in endgame_data.json while that phase is still listed there.
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
GENERIC = re.compile(r"^(Any\b|Main DPS|Sub-DPS|Support|Sustain|Healer|Shielder|.* Unit$)", re.I)
ALIASES = {"Topaz": "Topaz & Numby", "Permansor Terrae": "Dan Heng • Permansor Terrae"}
SLOTS = ("body", "feet", "sphere", "rope")


def load(name):
    with open(os.path.join(DATA, name), encoding="utf-8") as f:
        return json.load(f)


def main():
    game = load("game_data.json")
    builds = load("character_builds.json")
    chars, cones, sets = set(game["characters"]), set(game["light_cones"]), game["relic_sets"]
    planar = {n for n, s in sets.items() if "Planar Sphere" in (s.get("pieces") or {})}
    relic = set(sets) - planar
    errors = []

    def member_ok(text):
        for part in re.split(r"\s*/\s*", text):
            part = ALIASES.get(part, part)
            if part in chars or GENERIC.match(part):
                continue
            tb = re.match(r"^Trailblazer \((\w+)\)$", part)
            if tb and f"Trailblazer{tb.group(1)}#M" in chars:
                continue
            if any(c.endswith(f" • {part}") or c.startswith(f"{part} &") for c in chars):
                continue
            return False
        return True

    for name, b in builds.items():
        where = f"{name}:"
        if name not in chars:
            errors.append(f"{where} not a character in game_data.json")
        for lc in b.get("lightCones") or []:
            if lc.get("name") not in cones:
                errors.append(f"{where} unknown light cone '{lc.get('name')}'")
        for s in b.get("relicSets") or []:
            if s.get("name") not in relic:
                errors.append(f"{where} unknown relic set '{s.get('name')}'")
        for s in b.get("planarSets") or []:
            if s.get("name") not in planar:
                errors.append(f"{where} unknown planar set '{s.get('name')}'")
        missing = [k for k in SLOTS if not (b.get("mainStats") or {}).get(k)]
        if missing:
            errors.append(f"{where} main stats missing for {', '.join(missing)}")
        for team in b.get("teams") or []:
            for m in team.get("teamMembers") or []:
                if not member_ok(m):
                    errors.append(f"{where} team '{team.get('name')}' has unknown member '{m}'")

    teams_file = os.path.join(DATA, "endgame_teams.json")
    if os.path.exists(teams_file):
        endgame = load("endgame_teams.json").get("phases", {})
        try:
            known = {f"{m['id']}:{p['id']}" for m in load("endgame_data.json")["modes"] for p in m["phases"]}
        except (OSError, KeyError):
            known = None
        for key, phase in endgame.items():
            where = f"endgame {key}:"
            if not re.match(r"^(memory|story|boss|peak):\d+$", key):
                errors.append(f"{where} key must look like 'memory:1036'")
            if not (phase.get("source") or {}).get("url"):
                errors.append(f"{where} no source url")
            for pick in phase.get("picks") or []:
                for c in pick.get("characters") or []:
                    if not member_ok(c):
                        errors.append(f"{where} unknown character '{c}'")
            for team in phase.get("teams") or []:
                if len(team.get("members") or []) != 4:
                    errors.append(f"{where} team '{team.get('name')}' doesn't have 4 members")
                for m in team.get("members") or []:
                    if not member_ok(m):
                        errors.append(f"{where} team '{team.get('name')}' has unknown member '{m}'")
        if known is not None:
            old = sorted(k for k in endgame if k not in known)
            if old:
                print("Endgame phases no longer in endgame_data.json (can be removed): " + ", ".join(old))

    without = sorted(c for c in chars if c not in builds)
    print(f"{len(builds)} builds checked, {len(errors)} problems")
    print("Characters without a build: " + (", ".join(without) if without else "none"))
    for e in errors:
        print("  - " + e)
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
