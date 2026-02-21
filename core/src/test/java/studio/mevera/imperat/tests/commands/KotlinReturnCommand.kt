package studio.mevera.imperat.tests.commands

import studio.mevera.imperat.annotations.Command
import studio.mevera.imperat.annotations.Execute
import studio.mevera.imperat.tests.TestSource

@Command("kret")
class KotlinReturnCommand {

    @Execute
    fun run(src: TestSource): String {
        return "ok"
    }
}