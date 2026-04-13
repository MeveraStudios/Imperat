# AGENTS.md – Imperat AI Coding Agent Guide

## Project Overview

Imperat is a modular, high-performance Java command framework for Minecraft and related platforms. It provides a modern, annotation-driven approach to
command definition, supporting multiple backends (Paper, Velocity, Bungee, Minestom, JDA, Hytale, CLI, etc.) via subprojects. The core logic is in
`core/`, with platform-specific integrations in sibling modules.

## Architecture & Key Patterns

- **Core Module (`core/`)**: Contains the main command parsing, execution, context, and annotation logic. All platform modules depend on this.
- **Platform Modules**: Each of `paper/`, `velocity/`, `bukkit/`, `bungee/`, `minestom/`, `jda/`, `hytale/`, `cli/` provides integration for a
  specific server or environment. They adapt the core API to the platform's command/event system.
- **Command Definition**: Commands are defined as annotated Java classes using `@RootCommand`, `@SubCommand`, `@Execute`, `@Named`, `@Greedy`, etc.
  See `core/src/test/java/studio/mevera/imperat/tests/commands/complex/TestCommand.java` and `PartyCommand.java` for real-world patterns.
- **Command Tree**: Commands are parsed into a tree structure (`command/tree/`), supporting nested subcommands, argument inheritance, and advanced
  usage patterns.
- **Context & Execution**: Command execution context and argument/flag resolution are handled in `context/` and `command/`.
- **Help System**: The help system is query/render/send based. `CommandHelp` collects `HelpResult` data, `HelpRenderer` formats it, and `HelpSender`
  delivers it.

## Developer Workflows

- **Build**: Use Gradle. To build all modules: `./gradlew build` (or `gradlew.bat build` on Windows).
- **Test**: Run all tests with `./gradlew test`. Core logic is heavily tested in `core/src/test/java/` (see `integration/`, `commands/`,
  `subcommands/`).
  - Additional test tasks:
    - `./gradlew fastTest` – runs only fast unit tests
    - `./gradlew eventsTest` – runs only event system tests
    - `./gradlew integrationTest` – runs only integration tests
    - `./gradlew functionalTest` – runs all functional tests (excludes performance tests)
- **Publish**: Maven publishing is configured; see `build.gradle.kts` for details.
- **Debugging**: Use the test suite for regression and integration checks. Tests use a custom `TestCommandSource` and `BaseImperatTest` for
  assertions.

## Project-Specific Conventions

- **Annotation-Driven**: All command logic is annotation-based. Avoid manual registration; use annotations for all command, argument, and permission
  definitions.
- **Argument Inheritance**: Subcommands can inherit arguments from parent commands using `@InheritedArg`.
- **Default Values & Suggestions**: Use `@Default`, `@Suggest` for argument defaults and tab-completion.
- **Flags**: Both switch flags (boolean, e.g. `-s`, `-ip`) and value flags (e.g. `-m <msg>`, `-time <duration>`) are supported, including aliases and
  default values. See `BanCommand.java`, `Ban2Command.java`, `GitCommand.java`, `ShoutCommand.java`, and `FlagsAndSwitchesTest.java` for usage
  patterns and tests.
- **Help/Usage**: Each command should provide a help subcommand or usage info, leveraging the help system.
- **Permissions**: Permission checks are annotation-based and can be attached to commands or arguments.
- **Platform Adaptation**: Each platform module adapts the core API to its environment, but does not duplicate core logic.
- **Custom Argument Types**: You can define and register custom argument types (see `groupcommand/GroupArgument.java` and its usage in
  `AnnotatedGroupCommand.java`).
- **Custom Help Rendering**: The help system supports fully custom rendering and delivery (see `ExampleHelpRenderer.java` and its use in
  `AnnotatedGroupCommand.java`).
- **Kotlin Support**: Both Java and Kotlin are supported for commands and tests.

## Integration Points & Extensibility

- **Adding Platforms**: To add a new platform, create a new module and implement the necessary adapters for `CommandSource`, argument types, and event
  hooks.
- **Custom Argument Types**: Extend argument parsing by adding new types in `command/arguments/` and registering them in the config.
- **Responses & Help Rendering**: Extend responses in `responses/` and help rendering in `command/tree/help/` plus `command/tree/help/renderers/`.

## Key Files & Directories

- `core/src/main/java/studio/mevera/imperat/` – Core logic
- `core/src/test/java/studio/mevera/imperat/tests/` – Test suite (integration, real-world, and regression tests)
- `command/tree/help/` – Help query/result/sender APIs
- `build.gradle.kts` – Build and publishing configuration
- `README.md` – High-level overview and example

## Examples

- See `TestCommand.java` and `PartyCommand.java` for idiomatic command definitions.
- See also real-world examples: `BanCommand.java`, `Ban2Command.java`, `GitCommand.java`, `ShoutCommand.java`, `GiveCmd.java`, and the `groupcommand/`
  directory for advanced patterns (custom arguments, custom help renderers).
- Integration tests in `integration/IntegrationTest.java` and `flags/FlagsAndSwitchesTest.java` show real command usage and expected behaviors.

## External Resources

- [Official Documentation](https://mevera.studio/docs/Imperat/getting-started)
- [Discord Support](https://discord.gg/McN4GMWApE)

---
For more, see the documentation and test sources for up-to-date usage patterns and extension points.
