package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.util.ImperatDebugger;

@Command("kit")
public final class KitCommand {

    @SubCommand(
            "create"
    )
    public void createKit(TestSource source, @Named("kit") String kit, @Named("weight") @Optional @Default("1") Integer weight) {
        ImperatDebugger.debug("kit=%s, weight=%s", kit, weight);
    }
}