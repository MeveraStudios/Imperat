# Imperat v3 Comprehensive AI Context Guide

This document contains context for AI agents working with the **Imperat v3** command framework. It serves as a persistent knowledge base for generating syntactically correct command classes, including all advanced features like exception handling, suggestion providers, ranges, and cooldowns.

## 1. Core Package Structure

In version 3.6.0+, all command annotations are located in the following package:
`studio.mevera.imperat.annotations.types.*`

Do **NOT** use `studio.mevera.imperat.annotation.*` (singular) or `dev.velix.*`.

Important core classes for Bukkit:
```java
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.BukkitImperat; // Note: NOT studio.mevera.imperat.bukkit.BukkitImperat
```

## 2. Command Registration

The command framework must be registered in the `onEnable()` method of your Bukkit/Paper plugin:

```java
BukkitImperat imperat = BukkitImperat.builder(plugin).build();
imperat.registerCommand(new MyCommand());
```

## 3. Basic Commands & Subcommands

Imperat uses `@RootCommand` for the base command and `@SubCommand` or `@Execute` for routing logic.

*   `@RootCommand({"name", "alias"})` on the class.
*   `@Execute` marks the default/fallback execution method.
*   `@SubCommand({"name", "alias"})` marks a specific subcommand execution method.

```java
package com.example.plugin.command;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RootCommand({"economy", "eco"})
public class EconomyCommand {

    // Runs when just `/eco` is typed
    @Execute
    public void defaultExecution(CommandSender sender) {
        sender.sendMessage("Economy command usage...");
    }

    // Runs when `/eco give <player> <amount>` is typed
    @SubCommand({"give", "add"})
    public void give(CommandSender sender, Player target, double amount) {
        sender.sendMessage("Gave " + amount + " to " + target.getName());
    }
}
```

## 4. Arguments & Parameters

Imperat maps command arguments to Java method parameters automatically.

### Key Parameter Annotations

*   **`@Optional`**: Indicates an argument doesn't have to be provided.
*   **`@Greedy`**: Captures all remaining command line input as a single String.
*   **`@Default("value")`**: Provides a default string value if the argument is missing.
*   **`@Suggest({"val1", "val2"})`**: Provides static tab completion suggestions for an argument.

### Example with Arguments

```java
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Suggest;

@RootCommand({"message", "msg"})
public class MessageCommand {

    @Execute
    public void sendMessage(
        Player sender, 
        Player target, 
        @Suggest({"Hello!", "How are you?"}) @Greedy String message
    ) {
        target.sendMessage(sender.getName() + " says: " + message);
    }
}
```

## 5. Security & Context Annotations

*   **`@Permission("plugin.command.permission")`**: Restricts the command or subcommand.
*   **`@Async`**: Instructs the framework to run the command on an asynchronous thread, ideal for database operations.
*   **`@Cooldown`**: Applies a per-invocation cooldown to a command method to prevent spam.

```java
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.Async;
import studio.mevera.imperat.annotations.types.Cooldown;
import java.util.concurrent.TimeUnit;

@RootCommand({"admin"})
@Permission("plugin.admin")
public class AdminCommand {

    @SubCommand({"dbclean"})
    @Async
    @Cooldown(value = 10, unit = TimeUnit.SECONDS, permission = "admin.bypass.cooldown")
    public void cleanDatabase(CommandSender sender) {
        sender.sendMessage("Database cleaned.");
    }
}
```

## 6. Advanced Parameters: Constraints, Flags, and Switches

Imperat provides advanced modifiers for your parameters.

*   **`@Range(min = X, max = Y)`**: Constrains numeric parameters (like `int` or `double`) to specific bounds. Throws an error if out of bounds.
*   **`@Flag({"silent", "s"})`**: Declares an optional marker. Can capture a value (e.g. `-silent true`) or simply flag presence.
*   **`@Switch({"force", "f"})`**: Similar to `@Flag`, but explicitly for boolean toggles without requiring arguments (e.g., just `-f` or `-force`).

```java
import studio.mevera.imperat.annotations.types.Range;
import studio.mevera.imperat.annotations.types.Switch;

@SubCommand({"pay"})
public void payMoney(
    Player sender, 
    Player target, 
    @Range(min = 1.0, max = 10000.0) double amount,
    @Switch({"silent", "s"}) boolean silent
) {
    // If command is `/eco pay target 500 -silent`, silent = true.
    if (!silent) target.sendMessage("You got paid!");
}
```

## 7. Dynamic Suggestion Providers

For dynamic tab completions (like items from a database), use `@SuggestionProvider`.

First, define a custom provider implementing `SuggestionProvider<CommandSource>`:
```java
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import java.util.List;

public class CustomJobProvider implements SuggestionProvider<CommandSource> {
    @Override
    public List<String> provide(SuggestionContext<CommandSource> context, Argument<CommandSource> argument) {
        return List.of("Miner", "Lumberjack", "Farmer"); // Fetch dynamically!
    }
}
```
Then bind it in your command:
```java
import studio.mevera.imperat.annotations.types.SuggestionProvider;

@SubCommand({"join"})
public void joinJob(Player sender, @SuggestionProvider(CustomJobProvider.class) String job) {
    sender.sendMessage("Joined: " + job);
}
```

## 8. Exception Handling

Imperat can catch exceptions cleanly using `@ExceptionHandler`. The annotated method will automatically catch the specified exception if thrown inside the command.

```java
import studio.mevera.imperat.annotations.types.ExceptionHandler;
import org.bukkit.command.CommandSender;

@RootCommand({"test"})
public class TestCommand {

    @Execute
    public void run(CommandSender sender, int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Number cannot be negative!");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgument(CommandSender sender, IllegalArgumentException e) {
        sender.sendMessage("§cError: " + e.getMessage());
    }
}
```

## 9. Migration Notes & Crucial Rules
- Annotations shifted from `studio.mevera.imperat.annotation.*` to `studio.mevera.imperat.annotations.types.*`.
- `@Execute` arguments array `{"sub"}` was previously used for subcommands, but now `@SubCommand` is preferred.
- Always compile with the `-parameters` compiler flag (already configured in `build.gradle.kts`) so Imperat can read parameter names for auto-generating usage strings.
- The entry class is `BukkitImperat` from `studio.mevera.imperat.BukkitImperat`, do NOT use the `.bukkit` subpackage.
