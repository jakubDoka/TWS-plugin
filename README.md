# The worst mindustry plugin 2 

### Content

This plugin implements loadout system.Players can launch resources and store them for later use.
Plugin also implements a unit factory witch aleuts players to units from resources from loadout. 
Then there is also command for building cores for resources.

Plugin will create a default config and save file. In a config ,you can set up which units can be built, 
how mutch will they cost and speed of building. Save saves the loadout and factory progress.

### Building a Jar

CMD to your plugin folder and use `gradlew jar`(windows) `./gradlew jar`(Linux/MacOs)

Output jar should be in `build/libs`.

### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.

### Bugs

This plugin is far from done and can contain some bugs, if you find some please report them in [here](https://github.com/jakubDoka/TWS-plugin/issues/new).
The best way to report bug is explanation of how to recreate it.
