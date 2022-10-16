# PiLauncher
This is a simple launcher for Oculus Quest and Pico VR headsets supporting official/SideQuest apps and games.

[Builds](https://github.com/lvonasek/binary/tree/master/QuestPiLauncher),
[VIdeo](https://youtu.be/-CPBeUSL2Nw?t=4)
![Screenshot](https://github.com/lvonasek/QuestPiLauncher/blob/main/SCREENSHOT.png?raw=true)

## Compile the launcher
* Open **Launcher** in Android Studio
* Ensure USB debugging is enabled on your headset
* Hit play button in Android Studio

## Convert icons from SideQuest
* Collect JSON data from SideQuest (could be done using Chrome Debug Console)
* Join JSONs together
* Format the JSONs to be human readable
* Place the JSON into txt file into `SideIcons/localdb.json`
* Create `SideIcons/out/` folder
* Open **SideIcons** in Eclipse
* Run the project
