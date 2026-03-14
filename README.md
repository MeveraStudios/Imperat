<div align="center">
<img src="https://raw.githubusercontent.com/MeveraStudios/Imperat/refs/heads/master/assets/logo.png" alt="Imperat Logo" width="400"/> 
</div>

# Imperat - The Blazing Fast Command Framework

[![Maven Central](https://img.shields.io/maven-central/v/studio.mevera/imperat-core?style=for-the-badge&color=blue)](https://search.maven.org/artifact/studio.mevera/imperat-core)
[![Discord](https://img.shields.io/discord/1285395980610568192?style=for-the-badge&color=7289da&label=Discord)](https://discord.gg/McN4GMWApE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge)](https://java.com)

Imperat is a high performance generic command framework, designed to facilitate the creation of commands
and replace the old boilerplate code with new modern and well organized code for your commands!
**Built with ❤️**

## Example Command
```java

@RootCommand({"message", "msg"})
public class MessageCommand {

    @Execute()
    public void exec(Player sender, Player target, @Greedy String message) {
        // send private message to the target
        target.sendMessage("From " + sender.getName() + ": " + message);
    }
}
```

## 📚 Learn More

- [Documentation](https://mevera.studio/docs/Imperat/getting-started)
- [Discord](https://discord.gg/McN4GMWApE)



