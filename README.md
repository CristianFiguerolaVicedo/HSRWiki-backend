# Honkai: Star Rail Wiki

This project is meant to be a database for players of Honkai: Star Rail to see characters, lightcones, builds, relics and much more.

The game data is generated from [Mar-7th/StarRailRes](https://github.com/Mar-7th/StarRailRes) with an extractor based on [kel-z/HSR-Data](https://github.com/kel-z/HSR-Data) (that repository stopped updating at version 2.7). The data currently included is from **version 4.6**.

The project is made into 2 parts divided into 2 repositories, the backend and the frontend, the frontend is in the following link: [Frontend-Repository](https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend).

## Setting up the project

In order to use the project you have to first clone both repositories. To do that, open the terminal and navigate to the folder where you want to clone both repositories and execute `git clone https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend.git` for the frontend repository and `git clone https://github.com/CristianFiguerolaVicedo/HSRWiki-backend.git` for the backend repository.

After doing this, open both parts on your favourite code editor (Recommended: IntelliJIdea for the backend and Visual Studio Code for the frontend). On the backend part, open the project and run it (or run `./mvnw spring-boot:run`); it serves the API on `http://localhost:8080/api`. The game data is already included, so nothing else is needed. For the frontend, open the project and execute in the terminal `npm install` to install all the necessary dependencies of the project. After that, execute `npm run dev` and do `Ctrl + click` to on the link that will appear to open the project on the browser.

## Objective of the project

### Tecnichal objectives

The main purpose of this project on the technical ambit was to practice how to retrieve information from GitHub repositories, format it, extract it, work with it and return it how I needed it.

As well as select the information and how the information is retrieved from the repository.

### Web objectives

The main objective of this application is to be clear, appealing to watch and user-friendly. The main focus is to display the information in an easy and clear way for the user to see it without getting confused.

The colors are not to bright to be appealing to the sight and the info is disposed in a way that is easily identified.

## Backend parts

### Game data (data-pipeline)

The data the API serves lives in `src/main/resources/data/`:

- `game_data.json`: characters, light cones, relic sets and items, generated automatically.
- `character_builds.json`: the recommended builds, written by hand.

To update the game data after a new Honkai: Star Rail version (once StarRailRes has been updated), run from the repository root:

```
python data-pipeline/update_data.py
```

It downloads only the English index files of StarRailRes (a few MB, no images), runs the extractor in `data-pipeline/hsrdata/` and rewrites `game_data.json`. The extractor understands the newer kits: Remembrance and Elation paths, Memosprite and Elation skills, and the enhanced kits added from version 3.4.

The API also exposes `GET /api/version`, which returns the game version of the loaded data.

Configuration (`application.properties`):

- `hsrwiki.data-file` / `hsrwiki.builds-file`: where to read the data from (classpath by default; use `file:/path/...` for an external file).
- `hsrwiki.cors.allowed-origins`: frontend origins allowed to call the API.

### Custom files

In the main folder we can see many packages: Controller, dto, entities, models, services and utils.

#### Controllers

In this folder you can find the files with the operations like getting all the characters, getting lightcones by name, etc.

#### Dto

The dto's are the entities that are returned to the frontend

#### Entities

The structure of every part of the web. Each file contains the properties of every unit of the web, the properties of the characters, the relics, the lightcones, etc.

#### Service

Here we can find 2 things:

- First, the helper methods to convert to dto every entity

- Second, the GameDataLoader class.

The GameDataLoader file is the one in charge of retrieving the information from the json files generated before and parse it for the controllers to send it to the frontend.

Also has some helper methods to parse the information, like the skills of the characters or the methods that return all the characters, for example.

#### Util

Lastly, the util folder contains the enums for the paths and elements used. The builds file, `character_builds.json`, is in `src/main/resources/data/`.

This last file is the one containing the builds for every character in the game. It is not important to know it, but it was handtyped to ensure that the builds were as accurate as possible to the best ones.

## Deployment (Render, free plan)

1. Render → **New → Web Service** → this repository. Runtime **Docker**, region **Frankfurt**, instance **Free**, health check path `/api/version`.
2. Environment variable `HSRWIKI_CORS_ALLOWEDORIGINS` = the frontend's address (e.g. `https://hsrwiki.vercel.app,http://localhost:5173`).
3. Every push to `main` redeploys. On the free plan the service sleeps after 15 minutes without visits and takes about a minute to wake up.

The `Dockerfile` builds the jar with Maven and limits the JVM to fit in 512 MB. Render passes the port in `PORT` (`server.port=${PORT:8080}`).
