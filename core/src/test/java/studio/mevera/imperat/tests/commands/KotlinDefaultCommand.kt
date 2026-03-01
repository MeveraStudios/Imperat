package studio.mevera.imperat.tests.commands

import studio.mevera.imperat.annotations.types.Execute
import studio.mevera.imperat.annotations.types.Named
import studio.mevera.imperat.annotations.types.RootCommand
import studio.mevera.imperat.tests.TestSource

@RootCommand("kdef")
class KotlinDefaultCommand {

    @Execute
    fun run(source: TestSource, @Named("input") input: String = "hello") {
        source.reply("input=$input")
    }
}
