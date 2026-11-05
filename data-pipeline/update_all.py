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
for the commit message.
"""

import json
import os
import subprocess
import sys

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
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as f:
            f.write(f"version={version}\n")


if __name__ == "__main__":
    main()
