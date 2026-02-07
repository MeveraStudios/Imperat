pluginManagement {
    plugins {
        kotlin("jvm") version "2.3.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "Imperat"

include("core")
include("bukkit")
include("paper")
include("bungee")
include("velocity")
include("brigadier")
include("adventure")
include("cli")
include("minestom")
include("jda")
include("hytale")