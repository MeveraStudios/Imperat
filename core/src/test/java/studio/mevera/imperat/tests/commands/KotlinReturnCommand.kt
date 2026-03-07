package studio.mevera.imperat.tests.commands

import studio.mevera.imperat.annotations.types.Execute
import studio.mevera.imperat.annotations.types.RootCommand
import studio.mevera.imperat.tests.TestCommandSource

@RootCommand("kret")
class KotlinReturnCommand {

    @Execute
    fun run(src: TestCommandSource): String {
        return "ok"
    }
}