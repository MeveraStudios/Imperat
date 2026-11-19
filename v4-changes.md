# Imperat 4.1.0

## JDA: text commands

- Registered commands can also run from messages (`!ban @user 1d`): `JdaConfigBuilder.textCommands(JdaTextCommands)`. Off unless set.
  - Prefixes per message (usually per server), a mention of the bot as a prefix, and custom trigger words (`JdaTextTriggerResolver`) that map to any command path (`"ticket staff"`). A command's own names and aliases can be switched off as triggers.
  - The rest of the message is parsed like any text input, so subcommands, quotes, flags and greedy arguments work. Permissions are checked while executing, so a refusal reaches the `PermissionDeniedException` handler.
  - Needs the `MESSAGE_CONTENT` intent (mentions of the bot work without it).
- `JdaCommandSource` now comes from a slash command or a message. **Breaking:** `origin()` returns the `GenericEvent`; use `slashEvent()` (null for text) or `messageEvent()` (null for slash).
  - New: `isSlash()`, `isMessage()`, `label()`, `commandPath()` (written like a slash command's full name for both), `guild()`, `channel()`, `message()`, `findUser/findMember/findRole` (from slash options or mentions, then the cache).
  - Answering the same way for both: `send(MessageCreateData)` (public; replaces a slash command's "thinking…") and `sendPrivately(MessageCreateData)` (ephemeral for slash; for text, the configured `JdaPrivateReplier`).
  - Discord only allows ephemeral messages as an answer to an interaction, so text commands answer privately with `JdaPrivateReplier.revealButton()` (default: a "Show" button whose click answers ephemerally, to the author only), `directMessage(fallback)` or `inChannel()`.
- `SlashCommandInteractionEvent` and `MessageReceivedEvent` parameters throw `UnsupportedInvocationException` when the command ran the other way. `Guild` and `MessageChannel` parameters work for both.
- `User`, `Member` and `Role` arguments accept every mention form and raw IDs, and find entities the command already carries before the cache.

# Imperat 4.0.0

The big ideas and useful changes from **3.6.0** to **4.0.0**:

## Core

- Rebuilt command parsing around a new tree walker, with better branch selection and more useful parse failures.
- Added a cleaner command tree model with projections, parsed nodes, pathway matching, scoring, and visualization support.
- Added dedicated command tree helpers for parsing, suggestions, help queries, and closest-pathway lookup.
- Improved ambiguous-command detection and added explicit duplicate-flag errors.
- Added a modular configuration system: execution, dependencies, annotations, argument types, responses, error handlers, placeholders, return resolvers, and runtime behavior can now be configured independently.
- Added a first-class command registry and a simpler command execution pipeline.
- Added command-source mapping and delegating command sources for more flexible integrations.
- Added configurable error dispatching and expanded response-key support.
- Added coroutine support hooks for asynchronous command execution.
- Reworked argument parsing around a reusable cursor API.
- Added `SimpleArgumentType` for fixed-token custom arguments and `GreedyArgumentType` for remaining-input arguments.
- Added built-in argument types for durations, instants, dates, date-times, big decimals, big integers, paths, regular expressions, and URIs.
- Added fixed-arity and greedy parsing that can reserve tokens for later arguments and handle inline flags correctly.
- Improved optional arguments, arrays, collections, maps, futures, enums, strings, numbers, and flag parsing.
- Added fuzzy auto-completion with case-insensitive substring matching.
- Improved flag extraction, default values, multi-token flag values, nested switch scopes, and duplicate-flag detection.
- Kept cooldown support while moving it onto the new execution/configuration model.

## Bukkit and Brigadier

- Replaced the old Bukkit/Paper split with a capability-based Bukkit backend.
- Bukkit now detects the available server capabilities and chooses the best registration strategy automatically:
  - modern Paper Brigadier registration,
  - legacy Paper Brigadier registration,
  - Commodore registration,
  - or the plain Bukkit command map.
- Added runtime class probing so modern Paper integrations are only used when available.
- Added native Paper argument mappings and a suggestion bridge for server-side Brigadier types.
- Rebuilt Brigadier command projection and registration for more accurate parsing and tab completion.
- Improved aliases, permissions, greedy arguments, selectors, positional arguments, flags, multi-token input, and subtree sharing in Brigadier.
- Fixed suggestion ranges and made Brigadier error messages match the actual command input more closely.
- Added Bukkit support for online-only command arguments and improved player, offline-player, world, location, and target-selector parsing.
- Added a character stream for selector parsing and tightened selector field/operator parsing.
- Removed the standalone `paper` module; Paper support now lives in the modern Bukkit backend.

## Platform integrations

- Simplified the Bukkit, BungeeCord, CLI, Hytale, JDA, Minestom, and Velocity integrations to use the new core lifecycle and configuration APIs.
- Added JDA slash-command support for Imperat flags, including switches and typed options such as booleans, numbers, users, members, roles, and channels.
- Updated Hytale support for the newer server API and extended location rotation parsing with roll support.
- Updated platform argument types and source wrappers to use the new cursor and execution APIs.

## Dependencies and project setup

- Bumped the project version to `4.0.0`.
- Updated Adventure API to `5.1.1`.
- Updated Lombok, BungeeCord API, JDA, Hytale Server, and the Gradle toolchain resolver.
- Updated the Bukkit compile target to modern Paper API `1.21.4`.
- Prevented Kotlin and coroutine runtime packages from being bundled into the core shaded jar.
- Updated the logo reference and removed the old logo asset.
- Extended CI coverage for the v4 tree branch.

## Quality

- Added broad regression coverage for command parsing, branch failures, optional arguments, flags, greedy arguments, suggestions, source providers, argument-type ordering, selectors, Brigadier behavior, and real-world CLI commands.
- Added new integration tests for JDA slash-command flags and the rebuilt Bukkit Brigadier backend.
