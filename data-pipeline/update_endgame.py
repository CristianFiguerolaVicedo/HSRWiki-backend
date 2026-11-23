"""Builds src/main/resources/data/endgame_data.json: the endgame modes' current and upcoming phases.

Everything comes from the game data dump (DimbreathBot/TurnBasedGameData) shared with the other
dump-based scripts: for each mode, the phases with their dates, the phase effect and the buffs to
choose from, and every stage with the enemies of each half (name, level, weaknesses).

    Memory of Chaos       ChallengeGroupConfig (type Memory) + ChallengeMazeConfig
    Pure Fiction          ChallengeStoryGroupConfig + ChallengeStoryMazeConfig
    Apocalyptic Shadow    ChallengeBossGroupConfig + ChallengeBossMazeConfig
    Anomaly Arbitration   ChallengePeakGroupConfig + ChallengePeakConfig (no dates in the dump)

Stage -> enemies: EventIDList -> PlaneEvent -> StageConfig.MonsterList (one entry per wave)
-> MonsterConfig (name, weaknesses) + MonsterTemplateConfig (rank).

Recommended teams are not game data: they live in endgame_teams.json, written by hand.

Usage (from the repository root):

    python data-pipeline/update_endgame.py [--skip-fetch]
"""

import datetime
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from update_item_descriptions import CACHE, ROOT, clean, fetch  # noqa: E402

OUTPUT = os.path.join(ROOT, "src", "main", "resources", "data", "endgame_data.json")
GAME_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "game_data.json")

# Phases to keep per mode: the ones still running or upcoming (by end date), at most this many
KEEP = 3

ELEMENTS = {"Physical": "PHYSICAL", "Fire": "FIRE", "Ice": "ICE", "Thunder": "LIGHTNING",
            "Wind": "WIND", "Quantum": "QUANTUM", "Imaginary": "IMAGINARY"}
RANKS = {"Minion": "Minion", "MinionLv2": "Minion", "Elite": "Elite", "LittleBoss": "Boss", "BigBoss": "Boss"}

MODES = [
    {"id": "memory", "name": "Memory of Chaos",
     "about": "Twelve stages, each with two halves fought by two different teams. Defeat every wave "
              "within the cycle limit: the fewer cycles you use, the more stars you earn.",
     "group": "ChallengeGroupConfig", "maze": "ChallengeMazeConfig", "schedule": "ScheduleDataChallengeMaze",
     "tierce": "ChallengeMazeTierce", "stage": "Stage"},
    {"id": "story", "name": "Pure Fiction",
     "about": "Four stages full of weaker enemies that keep coming. Score points by defeating them within "
              "a few cycles: area damage is king. Pick one of the phase's buffs before each run.",
     "group": "ChallengeStoryGroupConfig", "maze": "ChallengeStoryMazeConfig",
     "schedule": "ScheduleDataChallengeStory", "tierce": "ChallengeStoryMazeTierce", "stage": "Stage"},
    {"id": "boss", "name": "Apocalyptic Shadow",
     "about": "Four difficulties, each with one powerful boss per half. Score points by breaking and "
              "damaging the boss within four cycles; each half has its own buffs to pick from.",
     "group": "ChallengeBossGroupConfig", "maze": "ChallengeBossMazeConfig",
     "schedule": "ScheduleDataChallengeBoss", "tierce": "ChallengeBossMazeTierce", "stage": "Difficulty"},
    {"id": "peak", "name": "Anomaly Arbitration",
     "about": "Three Knight battles and a final boss, each with its own rules. Teams can't repeat "
              "characters across the Knights, and the boss has a harder mode for the best rewards.",
     "stage": "Knight"},
]



def load(name):
    with open(os.path.join(CACHE, "ExcelOutput", f"{name}.json"), encoding="utf-8") as f:
        data = json.load(f)
    return data if isinstance(data, list) else list(data.values())


def fill_params(text, params):
    """Replaces the game's placeholders (#1[i]%, #2[f1]...) with the buff's values."""
    def repl(m):
        i, fmt, pct = int(m.group(1)) - 1, m.group(2), m.group(3)
        if i >= len(params):
            return m.group(0)
        v = params[i] * (100 if pct else 1)
        digits = int(fmt[1:]) if fmt.startswith("f") and fmt[1:].isdigit() else 0
        s = f"{v:.{digits}f}" if digits else f"{round(v):d}" if abs(v - round(v)) < 1e-6 else f"{v:g}"
        return s + ("%" if pct else "")
    return re.sub(r"#(\d+)\[(\w+)\](%?)", repl, text)


class Dump:
    def __init__(self):
        with open(os.path.join(CACHE, "TextMap", "TextMapEN.json"), encoding="utf-8") as f:
            self.text_map = json.load(f)
        self.buffs = {}
        for b in load("MazeBuff"):
            self.buffs.setdefault(b["ID"], b)          # several levels share an ID: level 1 comes first
        self.events = {}
        for e in load("PlaneEvent"):
            if e.get("StageID"):
                self.events.setdefault(e["EventID"], e["StageID"])
        self.stages = {s["StageID"]: s for s in load("StageConfig")}
        self.monsters = {m["MonsterID"]: m for m in load("MonsterConfig")}
        self.templates = {t["MonsterTemplateID"]: t for t in load("MonsterTemplateConfig")}

    def text(self, ref):
        return self.text_map.get(str(ref.get("Hash"))) if isinstance(ref, dict) else None

    def buff(self, buff_id):
        b = self.buffs.get(buff_id)
        if not b:
            return None
        name, desc = clean(self.text(b.get("BuffName"))), self.text(b.get("BuffDesc"))
        if not name and not desc:
            return None
        desc = clean(fill_params(desc, [p.get("Value", 0) for p in b.get("ParamList", [])])) if desc else None
        return {"name": name, "description": desc}

    def buff_list(self, ids):
        return [b for b in (self.buff(i) for i in ids or []) if b]

    def monster(self, monster_id, level):
        m = self.monsters.get(monster_id)
        if not m:
            return None
        t = self.templates.get(m.get("MonsterTemplateID")) or {}
        return {
            "id": monster_id,
            "name": clean(self.text(m.get("MonsterName"))) or clean(self.text(t.get("MonsterName"))) or f"Enemy {monster_id}",
            "rank": RANKS.get(t.get("Rank"), "Minion"),
            "level": level,
            "weaknesses": [ELEMENTS[w] for w in m.get("StanceWeakList", []) if w in ELEMENTS],
        }

    def waves(self, event_ids):
        """Enemies of a fight, wave by wave; the same enemy twice in a wave is counted, not repeated."""
        out = []
        for event_id in event_ids or []:
            stage = self.stages.get(self.events.get(event_id, event_id))
            if not stage:
                continue
            level = stage.get("Level")
            for wave in stage.get("MonsterList", []):
                enemies = {}
                for key in sorted(wave, key=lambda k: int(re.sub(r"\D", "", k) or 0)):
                    m = self.monster(wave[key], level)
                    if m:
                        enemies.setdefault(m["id"], {**m, "count": 0})["count"] += 1
                if enemies:
                    out.append(list(enemies.values()))
        return out

    def half(self, maze, n):
        return {
            "weaknesses": [ELEMENTS[w] for w in maze.get(f"DamageType{n}", []) if w in ELEMENTS],
            "waves": self.waves(maze.get(f"EventIDList{n}")),
        }


def tierce_stage(row, dump, title):
    """The extra stage of a phase (the "tierce" tables). Their field names are obfuscated and change
    between versions, so the values are found by their shape instead of their names."""
    events = weak = None
    for v in row.values():
        if isinstance(v, list) and v and all(isinstance(x, str) and x in ELEMENTS for x in v):
            weak = v
        elif isinstance(v, list) and v and all(isinstance(x, int) for x in v) and all(x in dump.events for x in v):
            events = v
    if not events:
        return None
    return {"name": title, "extra": True,
            "halves": [{"weaknesses": [ELEMENTS[w] for w in weak or []], "waves": dump.waves(events)}]}


def date(text):
    return text[:10] if text else None


def main():
    if "--skip-fetch" not in sys.argv:
        fetch()
    dump = Dump()
    today = datetime.date.today().isoformat()
    with open(GAME_DATA, encoding="utf-8") as f:
        version = json.load(f).get("version")

    modes = []
    for mode in MODES:
        out = {k: mode[k] for k in ("id", "name", "about")}
        out["phases"] = peak_phases(dump) if mode["id"] == "peak" else phases(dump, mode, today)
        modes.append(out)
        print(f"{mode['name']}: " + ", ".join(f"{p['name']} ({p.get('begin')} - {p.get('end')})" for p in out["phases"]))

    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump({"version": version, "generated": today, "modes": modes}, f, ensure_ascii=False, separators=(",", ":"))
    print(f"-> {os.path.relpath(OUTPUT, ROOT)} ({os.path.getsize(OUTPUT) // 1024} KB)")


def phases(dump, mode, today):
    schedules = {s["ID"]: s for s in load(mode["schedule"])}
    mazes = {}
    for m in load(mode["maze"]):
        mazes.setdefault(m["GroupID"], []).append(m)
    story_extra = {e["GroupID"]: e for e in load("ChallengeStoryGroupExtra")} if mode["id"] == "story" else {}
    story_turns = {e["ID"]: e for e in load("ChallengeStoryMazeExtra")} if mode["id"] == "story" else {}
    boss_extra = {e["GroupID"]: e for e in load("ChallengeBossGroupExtra")} if mode["id"] == "boss" else {}
    tierce = {}
    for row in load(mode["tierce"]):
        ids = [v for v in row.values() if isinstance(v, int)]
        if ids:
            tierce[ids[0]] = row

    horizon = (datetime.date.fromisoformat(today) + datetime.timedelta(days=180)).isoformat()
    found = []
    for g in load(mode["group"]):
        s = schedules.get(g.get("ScheduleDataID"))
        if not s or g.get("ChallengeGroupType") not in ("Memory", "Story", "Boss"):
            continue
        if date(s["EndTime"]) <= today or date(s["BeginTime"]) > horizon:
            continue  # over, or a placeholder far in the future (the dump has some dated 2033)
        phase = {"id": g["GroupID"], "name": clean(dump.text(g.get("GroupName"))) or str(g["GroupID"]),
                 "begin": date(s["BeginTime"]), "end": date(s["EndTime"])}
        effect = dump.buff(g.get("MazeBuffID"))
        phase["effects"] = [effect] if effect else []
        if mode["id"] == "story":
            extra = story_extra.get(g["GroupID"], {})
            phase["effects"] += dump.buff_list(extra.get("SubMazeBuffList"))
            phase["buffChoices"] = [{"label": "Pick one", "options": dump.buff_list(extra.get("BuffList"))}]
        if mode["id"] == "boss":
            extra = boss_extra.get(g["GroupID"], {})
            labels = ["First half", "Second half", "Starward Mode"]
            phase["buffChoices"] = [{"label": labels[i], "options": dump.buff_list(extra.get(f"BuffList{i + 1}"))}
                                    for i in range(3) if extra.get(f"BuffList{i + 1}")]
        stages = []
        for m in sorted(mazes.get(g["GroupID"], []), key=lambda x: x.get("Floor", 0)):
            st = {"name": f"{mode['stage']} {m.get('Floor')}", "halves": [dump.half(m, 1), dump.half(m, 2)]}
            if m.get("ChallengeCountDown"):
                st["cycles"] = m["ChallengeCountDown"]
            if story_turns.get(m["ID"], {}).get("TurnLimit"):
                st["cycles"] = story_turns[m["ID"]]["TurnLimit"]
            stages.append(st)
        t = tierce.get(g.get("TierceID"))
        extra_stage = tierce_stage(t, dump, "Starward Mode") if t else None
        if extra_stage:
            stages.append(extra_stage)
        phase["stages"] = stages
        found.append(phase)
    found.sort(key=lambda p: p["begin"])
    return found[:KEEP]


def peak_phases(dump):
    """Anomaly Arbitration: the newest phase in the dump (it changes with each game version)."""
    levels = {c["ID"]: c for c in load("ChallengePeakConfig")}
    bosses = {c["ID"]: c for c in load("ChallengePeakBossConfig")}
    groups = sorted(load("ChallengePeakGroupConfig"), key=lambda g: g["ID"])
    if not groups:
        return []
    g = groups[-1]
    stages = []
    for i, lid in enumerate(g.get("PreLevelIDList", []) + [g.get("BossLevelID")]):
        lv = levels.get(lid)
        if not lv:
            continue
        boss = lid == g.get("BossLevelID")
        stage = {"name": clean(dump.text(lv.get("Title"))) or f"Knight {i + 1}", "boss": boss,
                 "effects": dump.buff_list(lv.get("TagList")),
                 "halves": [{"weaknesses": [ELEMENTS[w] for w in lv.get("DamageType", []) if w in ELEMENTS],
                             "waves": dump.waves(lv.get("EventIDList"))}]}
        stages.append(stage)
        b = bosses.get(lid) if boss else None
        if b:
            stage["buffChoices"] = [{"label": "Pick one", "options": dump.buff_list(b.get("BuffList"))}]
            stages.append({"name": clean(dump.text(b.get("HardTitle"))) or "Hard mode", "boss": True, "extra": True,
                           "effects": dump.buff_list(b.get("HardTagList")),
                           "halves": [{"weaknesses": stage["halves"][0]["weaknesses"],
                                       "waves": dump.waves(b.get("HardEventIDList"))}]})
    return [{"id": g["ID"], "name": clean(dump.text(g.get("Title"))) or str(g["ID"]), "begin": None, "end": None,
             "effects": [], "stages": stages}]


if __name__ == "__main__":
    main()
