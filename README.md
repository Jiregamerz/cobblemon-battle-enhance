# Cobblemon Battle Enhance

A Fabric mod that adds real-time Pokemon battles with camera transitions and AI-controlled wild Pokemon.

## Features

- **Camera System**: Smooth transition to Pokemon view when battle starts
- **AI Controller**: Wild and NPC Pokemon fight back in real-time
- **HUD**: HP bars and floating damage numbers
- **TPP View**: Third-person camera behind Pokemon

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.5+
- Fabric API 0.102.0+
- Cobblemon 1.7.3+
- Java 21

## Installation

1. Install Java 21 from https://adoptium.net/
2. Install IntelliJ IDEA Community Edition
3. Clone this project
4. Run `./gradlew build`
5. Copy `build/libs/cobblemon-battle-enhance-1.0.0.jar` to your mods folder

## How to Use

1. Install the mod on both client and server
2. Find a wild Pokemon or encounter a trainer
3. Press the battle key (default: R) to start
4. Camera will transition to Pokemon view
5. Use WASD to move Pokemon
6. Use number keys 1-4 to use moves
7. Use Space to dodge
8. Press Escape to flee

## Building from Source

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

The JAR will be in `build/libs/`

## Project Structure

```
src/main/java/com/battleenhance/
├── BattleEnhanceMod.java      # Main mod entry point
├── BattleManager.java         # Battle coordination
├── camera/
│   └── CameraSystem.java      # Camera transitions
├── ai/
│   └── PokemonAIController.java  # Wild/NPC Pokemon AI
├── hud/
│   └── BattleHUD.java         # HP bars and damage numbers
└── mixin/
    ├── PlayerEntityMixin.java
    ├── WorldRendererMixin.java
    ├── ClientPlayerEntityMixin.java
    └── GameRendererMixin.java
```

## Next Steps

1. Integrate with Cobblemon's move system
2. Add Pokemon-specific abilities
3. Add battle animations
4. Add sound effects
5. Add network synchronization for multiplayer

## License

MIT
