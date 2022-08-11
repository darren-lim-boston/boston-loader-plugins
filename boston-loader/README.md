# What is this?

This plugin is used for building the schematic files in Minecraft. Specifically, look at the `CommandHandler.java` for information about what commands can be used to build the models in the game.

NOTE: this plugin, unlike `boston-simulations`, was not built with modularity in mind. It may be more difficult, if Java or Plugin development is new to you, to parse through this project, especially compared to the `boston-simulations` project. The below documentation assumes you are more familiar with this kind of development.

*You will likely not have to modify this plugin in order to modify 3D models.*

# How to load schematics into game

0) ensure world is empty (a pre-build superflat world is good!)

1) teleport to 0 0 0 

2) run `/boston build <schematic name>`

3) subsequent runs to `/boston build <schematic name>` will offset the tiles correctly, as long as the player remains in the server and the server does not reset

- wait for each build to finish before running the next

example set of commands:

        /boston build combined_A

        /boston build combined_B

        ...and so on

# How to build plugin

Simply run `mvn assembly:assembly -DdescriptorId=jar-with-dependencies` in the terminal. This will automatically place it in the server for you! It is allowed to let the server run while the plugin is updated; the plugin will update automatically.