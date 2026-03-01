package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.util.ImperatDebugger;

@RootCommand("kit")
public final class KitCommand {

    @SubCommand(
            "create"
    )
    public void createKit(TestSource source, @Named("kit") String kit, @Named("weight") @Optional @Default("1") Integer weight) {
        ImperatDebugger.debug("kit=%s, weight=%s", kit, weight);
    }
}