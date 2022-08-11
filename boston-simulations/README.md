# What is this?

Development plugin for Boston Minecraft game, aimed to teach students about climate change and mitigation in disaster scenarios. See more detailed [final presentation here](https://docs.google.com/presentation/d/18zuYoUCk2TTFHFkMG8E0f2ld2jXcxY5MAEFUtcCQzZ8/edit?usp=sharing).

If you are here, then you probably will want to modify the game, potentially fixing bugs that exist or adding new narratives and subgames to the game. You should be familiar with Java programming, as well as Minecraft plugin development, to work on this to the fullest. If you want to play the game, then check out [the server](https://github.com/darren-lim-boston/boston-loader-server) instead.

# Current game structure

Currently, the game is separated into 5 narrative sequences and 3 games. The respective files that hold the narrative/game are specified as well.

1) **Introduction** - the mayor will introduce the player to the City of Boston, and tour them around some notable areas ([link](src/main/java/narrative/narrative/IntroductionMayorNarrative.java))

2) **Flood game** - the player will have to save the park from a flood ([link](src/main/java/game/FloodGame.java))

3) **Flood reflection** - the player reflects on what to do during a flood with their Office of Emergency Management team ([link](src/main/java/narrative/narrative/PostFloodNarrative.java))

4) **Heat wave game** - the player will have to get residents to safety during a heat wave ([link](src/main/java/game/HeatWaveGame.java))

5) **Heat wave reflection** - the player reflects on what to do during a heat wave with their Office of Emergency Management team ([link](src/main/java/narrative/narrative/PostHeatWaveNarrative.java))

6) **Hurricane game** - the player will have to save the residents from a hurricane before it hits ([link](src/main/java/game/HurricaneGame.java))

7) **Hurricane reflection** - the player reflects on what to do during a hurricane with their Office of Emergency Management team ([link](src/main/java/narrative/narrative/PostHurricaneNarrative.java))

8) **Conclusion** - the mayor congratulates the player for their hard work, and the game ends with a scoreboard ([link](src/main/java/narrative/narrative/ConclusionMayorNarrative.java))

# How to modify an existing game or narrative

## Narratives

Follow the link to the specific game or narrative from above. For narratives, you can easily change the narrative being spoken by changing the strings that hold each narration. For instance, we can look at a specific line in the `IntroductionMayorNarrative.java` file:

        narration.add(new NarrationMessage(NAME_MAYOR, 7, "Welcome to the City of Boston! Today we will be celebrating our city's resilience and community as a city.", introDialogueSound));

- This line is adding a new message to the overall narration structure. It is being spoken by the mayor, and the mayor's name is specified by NAME_MAYOR (look at `NarrativeUtils.java`). The message plays for 7 seconds, and the message is "Welcome to the City...". Finally, when this message is spoken, the sound specified by `introDialogueSound` is played.

If you want to change the skins of the Mayor or OEM Team Members or their names, then you should navigate to [NarrationUtils.java](src/main/java/narrative/narrative/NarrativeUtils.java) and modify the variables there as needed. The URLs should specify a direct link to the .png file for the skin.

## Games

For games, you might have to read through the code more thoroughly in order to figure out what exactly you want to change. Reading `Game.java` might be helpful in understanding how the class is structured.

## Deploy changes

**After you make changes**, make sure you build the plugin and update the plugin in the server. One way to do this is to call `mvn package` from the `boston-simulations` directory, then copy the `target/BostonSimulations.jar` file into the `boston-loader-server/plugins/` folder, while the server is offline. Then, start the server.

# Project structure

The plugin is organized into packages that hold each its respective goal. An outline can be seen below:

        game/ # holds code needed to run the games
        master/ # holds code needed to run the entire game sequence as a whole
        narrative/ # holds code needed to run the narratives

The [`Game.java`](src/main/java/game/Game.java) file is an outline for what a game requires in order to run successfully. [`FloodGame.java`](src/main/java/game/FloodGame.java), [`HeatWaveGame.java`](src/main/java/game/HeatWaveGame.java), and [`Hurricane.java`](src/main/java/game/Hurricane.java) each extend the `Game.java` class, which allows them to be registered as a game. See `Game.java` to understand what required components are needed to make a game, and the three sub-games to see examples of what can be done.

Similarly, the [`Narrative.java`](src/main/java/narrative/Narrative.java) class is an outline for what a narrative should include. See the files under `narrative/narrative` for examples of what can be done, and see `narrative/narration` for what kinds of narration styles can be made. Note that each class within `narrative/narration` is also a subclass of the overarching [`Narration.java`](src/main/java/narrative/narration/Narration.java) class, which allows you to define your own Narration action if needed for the narratives. The `Narration.java` class allows actions like the multiple-choice dialog (defined in `NarrationMultipleChoice.java`) or the messages (defined in `NarrationMessage.java`) to be made.

Finally, there are some other classes that are also worth briefly describing:

## GameMaster.java

This file holds the order of the games and narratives. It also is the overarching class that controls an entire game sequence (narrations and subgames, together) from start to end.

## GenericEventHandler.java

In order for a game or narrative to hook onto a Minecraft event, it must use the GenericEventHandler.java. This includes shells of events that can be overwritten, and the events themselves are called from `GameMaster.java`.

## GameComponent.java

The `GameComponent.java` class is the generic class from which any Game or Narrative will implement. This is a more abstract shell of what a Game or Narrative should include.

## NarrativeEntity.java

Narratives can include NarrativeEntities, which are depicted as residents or the Mayor. Games can also use NarrativeEntities in order to be depicted as residents running around the world.

# How to modify the game world

You will notice that when a game is started, the world will reset according to what is placed in `world-backup`. If you want to modify the world that the players will see, for example to make City Hall textured or change the models, then you can follow the following steps:

1) Make sure you are an OP for the server. You can do this by typing `op <username>` in the command line where you started the server.

2) You will be using the `/simulation` command to perform actions. Make sure you are in the world-game world, and if not, teleport there by calling `/simulation world world-game`.

3) Make sure the world is a clean copy. If you want to reset the world to a clean slate, call `/simulation reset` and immediately afterwards, call `/simulation confirm`. You may need to log out and log back into Minecraft to see changes.

4) Modify the world!

5) When you are done, save your changes to the world-backup folder by calling `/simulation backup` and immediately afterwards, `/simulation confirm`.

# How to create a new Game

Following the description in **Project Structure**, you will need to create a new .java class file under `game/` and extend the `Game.java` class. Then, implement the abstract methods as a template. Finally, to see this new game shown in the real game, modify `master/GameMaster.java` by initializing the class under the `loadComponents` method, for instance placing it before IntroductionMayorNarrative.

# How to create a new Narrative

Following the description in **Project Structure**, you will need to create a new .java class file under `narrative/narrative/` and extend the `Narrative.java` class. Then, implement the abstract methods as a template. Finally, to see this new narrative shown in the real game, modify `master/GameMaster.java` by initializing the class under the `loadComponents` method, for instance placing it before IntroductionMayorNarrative.

# Ending notes

These platforms are made for the Boston Minecraft summer project, but by no means are they limited to the City of Boston or the games themselves. You should feel free to use the code to create your own games, narratives, or even better, maybe this code can help you build your own natural disaster awareness game for your own city!