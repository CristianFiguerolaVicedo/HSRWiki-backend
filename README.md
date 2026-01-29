# Honkai: Star Rail Wiki

This project is meant to be a database for players of Honkai: Star Rail to see characters, lightcones, builds, relics and much more.

The info was retrieved using the repository [Kel-Z HSR-Data Repository](https://github.com/kel-z/HSR-Data).

The project is made into 2 parts divided into 2 repositories, the backend and the frontend, the frontend is in the following link: [Backend-Repository](https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend).

## Setting up the project

In order to use the project you have to first clone both repositories. To do that, open the terminal and navigate to the folder where you want to clone both repositories and execute `git clone https://github.com/CristianFiguerolaVicedo/HSRWiki-frontend.git` for the frontend repository and `git clone https://github.com/CristianFiguerolaVicedo/HSRWiki-backend.git` for the backend repository.

After doing this, open both parts on your favourite code editor (Recommended: IntelliJIdea for the backend and Visual Studio Code for the frontend). On the backend part, open the project and run it. For the frontend, open the project and execute in the terminal `npm install` to install all the necessary dependencies of the project. After that, execute `npm run dev` and do `Ctrl + click` to on the link that will appear to open the project on the browser.

## Objective of the project

### Tecnichal objectives

The main purpose of this project on the technical ambit was to practice how to retrieve information from GitHub repositories, format it, extract it, work with it and return it how I needed it.

As well as select the information and how the information is retrieved from the repository.

### Web objectives

The main objective of this application is to be clear, appealing to watch and user-friendly. The main focus is to display the information in an easy and clear way for the user to see it without getting confused.

The colors are not to bright to be appealing to the sight and the info is disposed in a way that is easily identified.

## Backend parts

### hsr-data

This is a folder conteining everything needed to retrieve the data from the repository. In order to do it, after cloning the repository, go to the folder hsr-data and execute the command `python src/main.py` which creates the output folder "output".

Looking at the content of this folder there are several documents, each one named after the type of data which contains, but the main ones are: game_data.json, game_data_verbose.json and game_data_verbose_with_icons.json. This files contain the same information but in different depth, being just information, more extense information and extense information with icons.

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

Lastly, the util folder contains the enums for the paths and elements used and the character_builds.json.

This last file is the one containing the builds for every character in the game. It is not important to know it, but it was handtyped to ensure that the builds were as accurate as possible to the best ones.
