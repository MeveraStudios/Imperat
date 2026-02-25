package studio.mevera.imperat.tests.commands

import studio.mevera.imperat.annotations.Execute
import studio.mevera.imperat.annotations.Named
import studio.mevera.imperat.annotations.RootCommand
import studio.mevera.imperat.tests.TestSource

@RootCommand("kdef")
class KotlinDefaultCommand {

    @Execute
    fun run(source: TestSource, @Named("input") input: String = "hello") {
        source.reply("input=$input")
    }
}
