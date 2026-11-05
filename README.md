<div align="center">

# HSRWiki — Backend

**REST API for HSRWiki, an unofficial Honkai: Star Rail wiki with build guides and player tools.**

![Game data](https://img.shields.io/badge/game%20data-v4.6-B8820F)
![Java](https://img.shields.io/badge/Java-21-1F2A44?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Python](https://img.shields.io/badge/data%20pipeline-Python%203-2F6FAE?logo=python&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

### [🚂 Open the live site →](https://hsr-wiki-frontend.vercel.app)

[Live site](https://hsr-wiki-frontend.vercel.app) ·
[Live API](https://hsrwiki-backend.onrender.com/api/version) ·
[Frontend repository](https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend) ·
[API reference](#api-reference) ·
[Updating the game data](#updating-the-game-data) ·
[Deployment](#deployment)

<img src="https://raw.githubusercontent.com/CristianFiguerolaVicedo/HSRWiki-frontend/main/docs/screenshots/character.jpg" alt="HSRWiki character page" width="860">

</div>

---

## Live deployment

| | Address |
|---|---|
| Website | [hsr-wiki-frontend.vercel.app](https://hsr-wiki-frontend.vercel.app) |
| API | [hsrwiki-backend.onrender.com/api](https://hsrwiki-backend.onrender.com/api/version) |

> The API runs on Render's free plan: after 15 minutes without visits it goes to sleep, so the first request can take about a minute while it wakes up. After that it responds normally.

## Overview

This service serves every piece of game data the HSRWiki frontend shows: characters with their skills, Eidolons, traces, lore and recommended builds, light cones, relic sets, items, and the data behind the material planner and the stat calculator. It also looks up a player's public in-game showcase by UID through [Enka.Network](https://enka.network), so players can compare their own builds with the wiki's guides **without logging in or sharing any credentials**.

All game data is generated ahead of time by a small Python pipeline from public data sources and shipped as JSON with the application, so the API needs no database and starts in seconds.

## Features

- **Complete game data, up to version 4.6:** 98 characters (including enhanced kits, Memosprite and Elation skills), 170 light cones, 34 relic sets and 28 planar ornament sets, and 844 items with descriptions and what uses them.
- **Character lore:** the in-game Character Archive (details, stories and voice actors) for every character.
- **Build guides:** hand-written recommended light cones, relic sets, main and sub stats, and team compositions.
- **Player showcase:** `GET /api/profile/{uid}` reads a public showcase from Enka.Network, computes the exact relic stats and checks each character against the recommended build. Each response is cached for the time Enka.Network asks (at least 60 seconds).
- **Planner and calculator data:** ascension, trace and EXP costs, base stats per ascension phase, always-on bonuses from traces, light cones and sets, and relic main stat values.

## Tech stack

| Layer | Technology |
|---|---|
| API | Java 21, Spring Boot 4 (Spring MVC), Jackson 3, Lombok |
| Data pipeline | Python 3 (standard library only), git |
| Packaging | Maven, Docker (multi-stage image) |

## Architecture

```
  StarRailRes (game data, en)  ─┐
  TurnBasedGameData (text maps) ├─►  data-pipeline/*.py  ─►  src/main/resources/data/*.json
  character_builds.json (manual)┘                                       │
                                                                        ▼
                         Enka.Network  ◄──  Spring Boot API  ──►  /api/*  ──►  HSRWiki frontend
```

```
src/main/java/com/crifigvic/hsrwiki/
├── config/       CORS configuration
├── controller/   REST endpoints
├── dto/          Response models
├── entity/       Domain model (characters, skills, light cones, relics, items...)
├── model/        Raw game data container
├── service/      Data loading (GameDataLoader), builds, showcase lookups
└── util/         Path and element enums

src/main/resources/data/
├── game_data.json          Characters, light cones, relic sets and items (generated)
├── character_builds.json   Recommended builds (hand-written)
├── planner_data.json       Planner and stat calculator data (generated)
└── showcase_lookup.json    Id tables to translate Enka responses (generated)

data-pipeline/              Python scripts that generate the files above
```

## Getting started

### Requirements

- **Java 21** (the Maven wrapper downloads Maven for you)
- **Python 3.10+** and **git**, only if you want to regenerate the game data

### Run locally

```bash
git clone https://github.com/CristianFiguerolaVicedo/HSRWiki-backend.git
cd HSRWiki-backend

# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080/api`. Check that it's running with:

```bash
curl http://localhost:8080/api/version
```

Then start the [frontend](https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend), which expects the API at that address by default.

### Run with Docker

```bash
docker build -t hsrwiki-backend .
docker run -p 8080:8080 hsrwiki-backend
```

## Configuration

Every property in `application.properties` can be overridden with an environment variable (Spring's relaxed binding: upper case, dots become underscores, dashes are removed).

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `server.port` | `PORT` | `8080` | Port the API listens on |
| `hsrwiki.cors.allowed-origins` | `HSRWIKI_CORS_ALLOWEDORIGINS` | `http://localhost:5173,http://localhost:4173` | Frontend origins allowed to call the API, comma separated |
| `hsrwiki.data-file` | `HSRWIKI_DATAFILE` | `classpath:data/game_data.json` | Game data file (`file:/path/...` for an external one) |
| `hsrwiki.builds-file` | `HSRWIKI_BUILDSFILE` | `classpath:data/character_builds.json` | Recommended builds file |
| `hsrwiki.planner-file` | `HSRWIKI_PLANNERFILE` | `classpath:data/planner_data.json` | Planner and stat calculator data |
| `hsrwiki.showcase-lookup-file` | `HSRWIKI_SHOWCASELOOKUPFILE` | `classpath:data/showcase_lookup.json` | Id tables for showcase lookups |
| `hsrwiki.enka.base-url` | `HSRWIKI_ENKA_BASEURL` | `https://enka.network/api/hsr/uid` | Enka.Network endpoint |
| `hsrwiki.enka.user-agent` | `HSRWIKI_ENKA_USERAGENT` | `HSRWiki/1.0 (+repository URL)` | User-Agent sent to Enka.Network, as its API requires |

## API reference

All endpoints are `GET` and return JSON. They are relative to `http://localhost:8080` locally, or to `https://hsrwiki-backend.onrender.com` for the live API (for example [`/api/version`](https://hsrwiki-backend.onrender.com/api/version)).

| Endpoint | Description |
|---|---|
| `/api/version` | Game version of the loaded data |
| `/api/characters` | All characters with skills, Eidolons, traces, lore and build |
| `/api/characters/{id}` | One character by game id (e.g. `1005`) |
| `/api/characters/builds` | Recommended builds for every character |
| `/api/lightcones` | All light cones |
| `/api/lightcones/{id}` · `/api/lightcones/name/{name}` | One light cone by id or by name |
| `/api/relics` · `/api/relics/{name}` | Relic and planar sets, or one set by name |
| `/api/items` · `/api/items/{id}` · `/api/items/name/{name}` | Items, with descriptions and what uses them |
| `/api/planner` | Costs and stat tables for the material planner and the stat calculator |
| `/api/profile/{uid}` | A player's public showcase (9 or 10 digit UID), checked against the builds |

`/api/profile/{uid}` answers errors as `{"error": "..."}` with a matching status: `400` invalid UID, `404` unknown player, `429` too many lookups, `503` game maintenance, `502` Enka.Network unreachable.

## Updating the game data

### Automatically

The [`Update game data`](.github/workflows/update-game-data.yml) workflow runs **every Wednesday** (patch day) at 18:00 UTC. It regenerates all the data and, if anything changed, commits it to `main` as `Actualizacion de datos a la version X.Y.Z`, which makes Render redeploy the API. It can also be started by hand from the **Actions** tab (**Run workflow**). If the checks fail (for example, fewer characters than before), nothing is published and GitHub reports the failed run.

### By hand

```bash
python data-pipeline/update_all.py
```

`update_all.py` runs the individual scripts in the order they depend on each other, then checks the result:

1. `update_data.py`: characters, light cones, relics and items from StarRailRes
2. `update_item_descriptions.py`: item texts from the game data dump
3. `update_character_lore.py`: Character Archive texts (needs the character list from step 1)
4. `update_data.py --skip-fetch`: merges those texts into `game_data.json`
5. `update_planner_data.py`: planner and stat calculator data

The scripts download only the files they need (a sparse, blob-less git clone kept in `data-pipeline/.cache/`, which is ignored by git). Recommended builds live in `src/main/resources/data/character_builds.json` and are written by hand, so new characters show up with all their data and lore but without a build until one is added.

## Deployment

The repository includes a `Dockerfile` that builds the jar with Maven and keeps the JVM within 512 MB, so it runs on free container hosts.

**Render (free plan)**

1. **New → Web Service**, connect this repository.
2. Runtime **Docker**, region **Frankfurt**, instance **Free**, health check path `/api/version`.
3. Add the environment variable `HSRWIKI_CORS_ALLOWEDORIGINS` with the frontend's address, without a trailing slash (e.g. `https://hsrwiki.vercel.app,http://localhost:5173`).

Every push to `main` redeploys automatically. On the free plan the service sleeps after 15 minutes without traffic and takes about a minute to wake up.

## Credits

- [Mar-7th/StarRailRes](https://github.com/Mar-7th/StarRailRes): game data and images.
- [DimbreathBot/TurnBasedGameData](https://github.com/DimbreathBot/TurnBasedGameData): game text maps for item descriptions and character lore.
- [kel-z/HSR-Data](https://github.com/kel-z/HSR-Data): original data extractor, extended in `data-pipeline/hsrdata/`.
- [Enka.Network](https://enka.network): public player showcases.

## Disclaimer

HSRWiki is a fan-made project and is not affiliated with or endorsed by HoYoverse. Honkai: Star Rail and all related names, characters and images are trademarks and copyright of HoYoverse.
