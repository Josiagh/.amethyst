# AmethystClient (Fabric, Minecraft 1.21.11, Yarn)

Modules: Sprint, Freecam, Freelook, Fullbright, SusChunk. ClickGUI on Right Shift.
Keys: H = Freecam, J = Freelook, Right Shift = ClickGUI (rebindable in Controls).
GUI: left-click toggles a module, right-click expands settings, drag panel headers to move.

## Build
1. Generate a fresh template at https://fabricmc.net/develop (1.21.11, Yarn) and copy its
   gradlew, gradlew.bat and gradle/wrapper folder into this project, and update
   gradle.properties with the exact versions it gives you.
2. Run ./gradlew build
3. The jar is in build/libs/ (use the one without -sources). Drop it in your mods folder
   with Fabric API.
