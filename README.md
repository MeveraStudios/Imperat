<p><img src="https://github.com/VelixDevelopments/Imperat/blob/master/logo.png"  alt=""/></p><br>

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/fad48fc9b696419ba81f5a8571e5c29c)](https://app.codacy.com/gh/VelixDevelopments/Imperat/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/dev/velix/imperat-core/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/dev.velix/imperat-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build](https://github.com/VelixDevelopments/Imperat/actions/workflows/build.yml/badge.svg)](https://github.com/VelixDevelopments/Imperat/actions/workflows/build.yml)
[![Discord](https://discord.com/api/guilds/1285395980610568192/widget.png)](https://discord.velix.dev/)

# Imperat: A High-Performance, Platform-Agnostic Command Dispatching Framework

Imperat is a powerful and versatile command dispatching framework built in Java, designed for optimal performance and broad compatibility. 
It provides a robust solution for managing and executing commands across various platforms, making it ideal for applications ranging from microservices to game engines.

## Key Features:

*   **Generic Command Dispatching:** Seamlessly dispatch commands across diverse platforms and systems.
*   **High Performance:** Engineered to handle massive volumes of commands with minimal overhead, ensuring efficient execution.
*   **Platform-Agnostic:** Imperat is designed to work across different environments, offering flexible integration with platforms such as Bukkit, Minestom, Velocity, Bungeecord, and CLI (Command Line Interface).
*   **Smart Usage Resolution (`SmartUsageResolve`):** This intelligent algorithm enables Imperat to handle complex command graphs, including middle optional arguments, providing a sophisticated approach to command parsing and execution.

*   **Comprehensive API:** Imperat offers a rich set of APIs for various functionalities, including:
    *   **Dispatcher API:** For managing command dispatching.
    *   **Command API (Classic & Annotations):** Flexible options for defining commands.
    *   **Error Handlers:** Robust error handling for command executions.
    *   **Parameter Type & Context/Suggestion Resolvers:** Advanced features for command argument parsing and auto-completion.
    *   **Processors:** For custom command processing logic.
    *   **CommandHelpProvider API:** Provides automation for help messages ***(+/- pagination)***
    *   **Source Resolvers:** For applying custom command sources.

## Installation:

Imperat is available on Maven Central. You can easily integrate it into your project using Maven or Gradle.

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>dev.velix</groupId>
    <artifactId>imperat-core</artifactId>
    <version>VERSION</version>
  </dependency>
  <dependency>
    <groupId>dev.velix</groupId>
    <artifactId>imperat-PLATFORM</artifactId>
    <version>VERSION</version>
  </dependency>
</dependencies>
```

### Gradle

```gradle
dependencies {
    implementation "dev.velix:imperat-core:VERSION"
    implementation "dev.velix:imperat-PLATFORM:VERSION"
}
```

Replace `PLATFORM` with your desired platform (e.g., `bukkit`, `minestom`, `velocity`, `bungeecord`, `cli`) , <br>
and `VERSION` with the latest version available.

## Example: Teleporting Players with Style

Let's imagine a Minecraft server where administrators want a powerful yet intuitive teleportation command. <br>
Imperat makes this easy with its flexible command definition and parameter resolution capabilities.

```java
import annotations.imperat.studio.mevera.Command;
import annotations.imperat.studio.mevera.Named;
import annotations.imperat.studio.mevera.Optional;
import annotations.imperat.studio.mevera.Usage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

// Assuming Imperat is initialized and registered
@Command({"teleport", "tp"})
public class TeleportCommand {

    @Usage
    public void teleportPlayerToPlayer(
            Player sender,
            @Named("target") Player target,
            @Optional @Named("destination") Player destinationPlayer
    ) {
        if (destinationPlayer != null) {
            // Teleport target to destination player
            target.teleport(destinationPlayer.getLocation());
            sender.sendMessage(target.getName() + " has been teleported to " + destinationPlayer.getName() + ".");
        } else {
            // Teleport sender to target player
            sender.teleport(target.getLocation());
            sender.sendMessage("You have been teleported to " + target.getName() + ".");
        }
    }

    @Usage
    public void teleportPlayerToCoordinates(
            Player sender,
            @Named("target") Player target,
            @Named("x") Double x,
            @Named("y") Double y,
            @Named("z") Double z
    ) {
        // Teleport target to coordinates
        target.teleport(new Location(target.getWorld(), x, y, z));
        sender.sendMessage(target.getName() + " has been teleported to X:" + x + ", Y:" + y + ", Z:" + z + ".");
    }

    @Usage
    public void teleportSelfToCoordinates(
            Player sender,
            @Named("x") Double x,
            @Named("y") Double y,
            @Named("z") Double z
    ) {
        // Teleport sender to coordinates
        sender.teleport(new Location(sender.getWorld(), x, y, z));
        sender.sendMessage("You have been teleported to X:" + x + ", Y:" + y + ", Z:" + z + ".");
    }
}

```

This example demonstrates Imperat's ability to:

*   **Define root commands with `@Command`:** Easily set the command name and aliases (e.g., `/teleport` or `/tp`).
*   **Handle multiple command usages with `@Usage`:** The `teleport` command can be used in various ways (e.g., `/tp <player>`, `/tp <player> <player>`, `/tp <player> <x> <y> <z>`, `/tp <x> <y> <z>`).
*   **Utilize platform-specific sources:** The `Player` is automatically injected, allowing interaction with the bukkit player.
*   **Support optional arguments using `@Optional` or `@Default`:** Clearly define required/optional command arguments with custom names using `@Named`
*   **Leverage automatic parameter resolution:** Imperat automatically resolves `Player` and `Double` types, simplifying command logic.

This flexibility allows developers to create powerful and user-friendly commands with clean, readable code, thanks to Imperat's intelligent command dispatching and parameter handling.

## Documentation & Community:

For detailed usage instructions, architecture overviews, and API documentation, visit the official docs website.

**Documentation:** https://docs.velix.dev/

Join our community on [Discord](https://discord.gg/McN4GMWApE) to connect with other developers, ask questions, and share ideas.

## License:

Imperat is released under the MIT License. See the `LICENSE` file in the repository for more information.
