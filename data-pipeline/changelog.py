"""The wiki's changelog (src/main/resources/data/changelog.json), shown on the website's Updates page.

Entries are grouped by day: adding items for a date that already has an entry adds them to it.
Each entry has sections by type, and each item is a line of text with an optional link:

    {"entries": [
      {"date": "2026-11-11", "version": "4.7.0", "sections": [
        {"type": "data", "items": [{"text": "New character", "ref": {"kind": "character", "name": "Kafka"}}]},
        {"type": "builds", "items": [{"text": "Relic set: Eagle of Twilight Line -> Prisoner in Deep Confinement",
                                      "ref": {"kind": "character", "name": "Kafka"}}]}
      ]}
    ]}

Section types: data (game data), builds, tierlist, world (planets and factions), site (website changes).
ref kinds: character, lightcone, relic (names exactly as in game_data.json), planet, faction.

Command line (used by the weekly builds task):

    python data-pipeline/changelog.py add --type builds --text "New build" --ref character:Kafka
    python data-pipeline/changelog.py show
"""

import argparse
import datetime
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILE = os.path.join(ROOT, "src", "main", "resources", "data", "changelog.json")
GAME_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "game_data.json")
TYPES = ["data", "builds", "tierlist", "world", "site"]
KINDS = ["character", "lightcone", "relic", "planet", "faction"]


def load():
    if not os.path.exists(FILE):
        return {"entries": []}
    with open(FILE, encoding="utf-8") as f:
        return json.load(f)


def save(log):
    log["entries"].sort(key=lambda e: e["date"], reverse=True)
    with open(FILE, "w", encoding="utf-8") as f:
        json.dump(log, f, ensure_ascii=False, indent=1)
        f.write("\n")


def current_version():
    try:
        with open(GAME_DATA, encoding="utf-8") as f:
            return json.load(f).get("version")
    except OSError:
        return None


def add(section_type, items, date=None, version=None, title=None):
    """Adds items ([{"text", "ref"?}]) to the entry of `date` (today by default)."""
    if section_type not in TYPES:
        raise ValueError(f"unknown section type {section_type!r}, use one of {TYPES}")
    if not items:
        return
    date = date or datetime.date.today().isoformat()
    log = load()
    entry = next((e for e in log["entries"] if e["date"] == date), None)
    if entry is None:
        entry = {"date": date, "version": version or current_version(), "sections": []}
        log["entries"].append(entry)
    if version:
        entry["version"] = version
    if title:
        entry["title"] = title
    section = next((s for s in entry["sections"] if s["type"] == section_type), None)
    if section is None:
        section = {"type": section_type, "items": []}
        entry["sections"].append(section)
        entry["sections"].sort(key=lambda s: TYPES.index(s["type"]))
    for item in items:
        if item not in section["items"]:
            section["items"].append(item)
    save(log)


def parse_ref(text):
    if not text:
        return None
    kind, _, name = text.partition(":")
    if kind not in KINDS or not name:
        raise ValueError(f"ref must look like kind:name with kind in {KINDS}")
    return {"kind": kind, "name": name}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    a = sub.add_parser("add", help="add one line to today's entry")
    a.add_argument("--type", required=True, choices=TYPES)
    a.add_argument("--text", required=True)
    a.add_argument("--ref", help="kind:name, e.g. character:Kafka")
    a.add_argument("--date", help="YYYY-MM-DD (default: today)")
    sub.add_parser("show", help="print the latest entry")
    args = parser.parse_args()

    if args.command == "add":
        item = {"text": args.text}
        ref = parse_ref(args.ref)
        if ref:
            item["ref"] = ref
        add(args.type, [item], date=args.date)
        print(f"Added to {args.date or datetime.date.today().isoformat()}: [{args.type}] {args.text}")
    else:
        entries = load()["entries"]
        print(json.dumps(entries[0] if entries else {}, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
