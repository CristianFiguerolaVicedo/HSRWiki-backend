"""Updates every generated data file in the right order and checks the result.

    python data-pipeline/update_all.py

Order (each step needs the previous one):
  1. update_data.py              new characters, light cones, relics and items from StarRailRes
  2. update_item_descriptions.py item texts from the game data dump
  3. update_character_lore.py    Character Archive texts (needs the character list from step 1)
  4. update_data.py --skip-fetch merges the texts of steps 2 and 3 into game_data.json
  5. update_planner_data.py      planner and stat calculator data

Afterwards it compares the new data with the previous one and exits with an error if something
looks broken (fewer characters than before, missing version...), so an automatic run never
publishes damaged data. On success it prints the game version, which the GitHub workflow uses
for the commit message, and notes what's new (version, characters, light cones, relic sets) in
the changelog shown on the website's Updates page.
"""

import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import changelog  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
CHECKED = ["characters", "light_cones", "relic_sets", "items"]


def load(name):
    with open(os.path.join(DATA, name), encoding="utf-8") as f:
        return json.load(f)


def run(script, *args):
    print(f"\n--- {script} {' '.join(args)}", flush=True)
    subprocess.run([sys.executable, os.path.join(HERE, script), *args], cwd=ROOT, check=True)


def note_changes(before, after):
    """Adds what's new in the game data to today's changelog entry (nothing if nothing changed)."""
    items = []
    if before.get("version") != after.get("version"):
        items.append({"text": f"Game data updated to version {after['version']}"})
    kinds = [("characters", "character", "New character"), ("light_cones", "lightcone", "New light cone"),
             ("relic_sets", "relic", "New relic set")]
    for key, kind, text in kinds:
        for name in sorted(set(after.get(key, {})) - set(before.get(key, {}))):
            items.append({"text": text, "ref": {"kind": kind, "name": name}})
    if items:
        changelog.add("data", items, version=after["version"])
        print(f"Changelog: {len(items)} new lines")


def main():
    before = load("game_data.json")

    run("update_data.py")
    run("update_item_descriptions.py")
    run("update_character_lore.py", "--skip-fetch")
    run("update_data.py", "--skip-fetch")
    run("update_planner_data.py", "--skip-fetch")

    # Every generated file must still be valid JSON
    after = load("game_data.json")
    for name in ("planner_data.json", "showcase_lookup.json"):
        load(name)

    problems = []
    if not after.get("version"):
        problems.append("game_data.json has no version")
    for key in CHECKED:
        old, new = len(before.get(key, {})), len(after.get(key, {}))
        print(f"{key}: {old} -> {new}")
        if new < old:
            problems.append(f"{key} went down from {old} to {new}")
    no_lore = [n for n, c in after["characters"].items() if "lore" not in c]
    if no_lore:
        print("Characters without archive texts yet: " + ", ".join(no_lore))

    if problems:
        print("\nNOT OK: " + "; ".join(problems))
        sys.exit(1)

    version = after["version"]
    print(f"\nOK: game data version {version}")
    note_changes(before, after)
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as f:
            f.write(f"version={version}\n")


if __name__ == "__main__":
    main()
