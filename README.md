<div align="center">

<img src="https://raw.githubusercontent.com/MeveraStudios/Imperat/refs/heads/master/assets/logo.png" alt="Imperat Logo" width="600"/>

# **Imperat** - The Blazing Fast Command Framework ⚡

[![Maven Central](https://img.shields.io/maven-central/v/studio.mevera/imperat-core?style=for-the-badge&color=blue)](https://search.maven.org/artifact/studio.mevera/imperat-core)
[![License](https://img.shields.io/badge/License-Custom-green?style=for-the-badge)](LICENSE)
[![Discord](https://img.shields.io/discord/1285395980610568192?style=for-the-badge&color=7289da&label=Discord)](https://discord.gg/McN4GMWApE)
[![Documentation](https://img.shields.io/badge/Docs-Available-brightgreen?style=for-the-badge)](https://docs.mevera.studio/Imperat)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge)](https://java.com)

**The most performant, feature-rich command framework for Java applications**

[📚 **Documentation**](https://docs.mevera.studio/Imperat) • [💬 **Discord**](https://discord.gg/McN4GMWApE) • [🚀 **Get Started**](#-quick-start) • [✨ **Features**](#-features) • [📊 **Benchmarks**](#-performance)

</div>

---

## 🎯 **Why Imperat?**

Imperat isn't just another command framework—it's the **ultimate solution** for developers who demand both **blazing performance** and **rich features**. <br>
Built by [Mqzen](https://github.com/Mqzen), Imperat delivers sub-microsecond command execution while maintaining an elegant, intuitive API.

<div align="center">

</div>

---

### 📊 **Performance**
Imperat provides the optimum performance for command execution and suggestion providing.<br>
We have proven this  through running benchmarks using **JMH**:

<img src="https://raw.githubusercontent.com/MeveraStudios/Imperat/refs/heads/master/assets/performance_chart.png" alt="Comparison performance chart"/>

| **Lightning Fast** ⚡ | **Feature Complete** 🎨 | **Multi-Platform** 🌍 |
|:---:|:---:|:---:|
| **29x faster** than Cloud<br/>**10x faster** than Lamp | Annotations, builders, suggestions,<br/>permissions, and much more | Bukkit, Velocity, BungeeCord,<br/>Minestom, CLI, and more |


| Framework | Median Latency | Throughput | vs Imperat |
|:---------:|:--------------:|:----------:|:----------:|
| **Imperat** ⚡ | **470ns** | **2.14M/sec** | **Baseline** |
| Lamp | 5,016ns | 199K/sec | 10x slower |
| Cloud | 12,208ns | 82K/sec | 29x slower |

<sub>*Benchmarked on complex command trees with 10+ depth levels and multiple branches*</sub>

> 💡 **What does this mean?** On a busy Minecraft server with many players, Imperat adds only **0.47ms** overhead per 1000 commands, while Other framework like Cloud adds **12.2ms**—that's the difference between smooth gameplay and noticeable lag!

---

## ✨ **Features**

<div align="center">

|     **Core Features**      |    **Advanced Features**     | **Developer Experience** |
|:--------------------------:|:----------------------------:|:------------------------:|
| Annotation-based commands  |   Async command execution    |     Zero boilerplate     |
|    Builder pattern API     | Tab completion & suggestions | Extensive documentation  |
|   Unlimited subcommands    |    Permission management     | IDE autocomplete support |
|    Parameter validation    |     Dependency injection     |  Custom parameter types  |
| Multiple usage patterns |     Processing pipeline      |    Hot-reload support    |
|    Greedy parameters    |      Command cooldowns       |    Context resolvers     |

</div>

---

## 🚀 **Quick Start**

### **Step 1: Add Imperat to Your Project**

<details open>
<summary><b>Maven</b></summary>

```xml
<dependency>
    <groupId>studio.mevera</groupId>
    <artifactId>imperat-core</artifactId>
    <version>%LATEST_VERSION%</version>
</dependency>

<!-- Add your platform module (e.g., for Bukkit) -->
<dependency>
    <groupId>studio.mevera</groupId>
    <artifactId>imperat-bukkit</artifactId>
    <version>%LATEST_VERSION%</version>
</dependency>
```

</details>

<details>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
dependencies {
    implementation("studio.mevera:imperat-core:%LATEST_VERSION%")
    // Add your platform module (e.g., for Bukkit)
    implementation("studio.mevera:imperat-bukkit:%LATEST_VERSION%")
}
```

</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
dependencies {
    implementation 'studio.mevera:imperat-core:%LATEST_VERSION%'
    // Add your platform module (e.g., for Bukkit)
    implementation 'studio.mevera:imperat-bukkit:%LATEST_VERSION%'
}
```

</details>

### **Step 2: Initialize Imperat**

```java
public class YourPlugin extends JavaPlugin {
    private BukkitImperat imperat;

    @Override
    public void onEnable() {
        // Create Imperat instance with builder pattern
        this.imperat = BukkitImperat.builder(this)
            .applyBrigadier(true)  // Enhanced suggestions on 1.13+
            .build();
        
        // Register your commands
        imperat.registerCommand(new GameModeCommand());
    }
}
```

### **Step 3: Create Your First Command**

```java
@Command({"gamemode", "gm"})
@Permission("server.gamemode")
@Description("Change player gamemode")
public class GameModeCommand {

    @Usage
    public void defaultUsage(
            Player source,
            @Named("mode") GameMode gameMode,
            @Optional @Named("target") Player target
    ) {
        // Handle: /gamemode <mode> [target]
        Player finalTarget = target != null ? target : source;
        finalTarget.setGameMode(gameMode);
        
        source.sendMessage("§aGamemode updated to " + gameMode.name());
        if (target != null && target != source) {
            target.sendMessage("§aYour gamemode was updated by " + source.getName());
        }
    }
    
    // Convenient aliases
    @Command("gmc")
    public void creative(Player source, @Optional Player target) {
        defaultUsage(source, GameMode.CREATIVE, target);
    }
    
    @Command("gms")
    public void survival(Player source, @Optional Player target) {
        defaultUsage(source, GameMode.SURVIVAL, target);
    }
}
```

That's it! You've just created a fully-functional command with:
- ✅ Multiple aliases (`/gamemode`, `/gm`, `/gmc`, `/gms`)
- ✅ Tab completion for GameMode and online players
- ✅ Optional parameters with smart defaults
- ✅ Permission checking
- ✅ Automatic help generation

---

## 🎨 **Advanced Example - Complex Command Trees**

<details>
<summary><b>Click to see a real-world rank management system</b></summary>

```java
@Command({"rank", "group"})
@Permission("server.rank")
@Description("Complete rank management system")
public class RankCommand {
    
    @Dependency
    private RankManager rankManager;
    
    @Usage
    public void help(CommandSource source, CommandHelp help) {
        help.display(source);  // Auto-generated help menu
    }
    
    @SubCommand("create")
    @Permission("server.rank.create")
    public void createRank(
            CommandSource source,
            @Named("name") String rankName,
            @Optional @Default("0") @Named("weight") int weight
    ) {
        Rank rank = rankManager.createRank(rankName, weight);
        source.reply("§aCreated rank: " + rank.getColoredName());
    }
    
    @SubCommand("delete")
    @Permission("server.rank.delete")
    public void deleteRank(
            CommandSource source,
            @Named("rank") Rank rank,  // Custom parameter type!
            @Switch("confirm") boolean confirm
    ) {
        if (!confirm) {
            source.error("§cAdd --confirm to delete this rank");
            return;
        }
        
        rankManager.deleteRank(rank);
        source.reply("§aDeleted rank: " + rank.getName());
    }
    
    @SubCommand("give")
    @Permission("server.rank.give")
    @Cooldown(value = 5, unit = TimeUnit.SECONDS)
    public void giveRank(
            CommandSource source,
            @Named("player") Player target,
            @Named("rank") Rank rank,
            @Optional @Named("duration") Duration duration
    ) {
        rankManager.setPlayerRank(target, rank, duration);
        source.reply("§aGave " + rank.getColoredName() + " §ato " + target.getName());
    }
}
```

</details>

---

## 🔧 **Platform Support**

<div align="center">

| Platform | Module | Status |
|:--------:|:------:|:------:|
| **Bukkit/Spigot/Paper** | `imperat-bukkit` | ✅ Stable |
| **Velocity** | `imperat-velocity` | ✅ Stable |
| **BungeeCord** | `imperat-bungee` | ✅ Stable |
| **Minestom** | `imperat-minestom` | ✅ Stable |
| **CLI Applications** | `imperat-cli` | ✅ Stable |
| **Discord (JDA)** | `imperat-jda` | 🚧 Coming Soon |
| **Sponge** | `imperat-sponge` | 🚧 Planned |

</div>

---

## 🎯 **Key Features Explained**

### **⚡ Blazing Fast Performance**
- **Sub-microsecond execution**: 470ns median latency
- **Linear O(n) scaling**: Consistent performance even with deep command trees
- **Minimal allocations**: Optimized memory usage

### **🎨 Flexible Command Creation**
- **Annotations**: Clean, declarative command structure
- **Builder API**: Dynamic command generation
- **Mixed approach**: Use both patterns in the same project

### **🔌 Rich Parameter System**
- **Custom types**: Register your own parameter types
- **Validation**: Built-in `@Range`, `@Values`, and custom validators
- **Greedy parameters**: `@Greedy` for multi-word inputs
- **Flags & switches**: `--flag value` and `--switch` support

### **🛡️ Advanced Permission System**
- **Hierarchical permissions**: Command, subcommand, and parameter-level
- **Auto Permission Assignment**: Generate permission nodes automatically
- **Custom permission checks**: Implement complex permission logic

### **📊 Processing Pipeline**
- **Pre-processors**: Validate, log, or modify before execution
- **Post-processors**: Handle results, logging, or cleanup
- **Exception resolvers**: Centralized error handling

---

## 🤝 **Contributing**

We welcome contributions! Whether it's bug reports, feature requests, or pull requests.

## 📜 **License**

Imperat is licensed under the [MIT License](LICENSE).

---

<div align="center">

### **Ready to supercharge your commands?**

[📚 **Read the Docs**](https://docs.mevera.studio/Imperat) • [💬 **Join our Discord**](https://discord.gg/McN4GMWApE) • [⭐ **Star on GitHub**](https://github.com/MeveraStudios/Imperat)

**Built with ❤️ by [Mqzen](https://github.com/Mqzen) and [iiAhmedYT](https://github.com/iiAhmedYT)**

</div>
