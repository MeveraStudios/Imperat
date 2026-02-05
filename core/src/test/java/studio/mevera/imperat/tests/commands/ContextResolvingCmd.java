package studio.mevera.imperat.tests.commands;

import org.junit.jupiter.api.Assertions;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;
import studio.mevera.imperat.tests.contextresolver.PlayerData;

@Command("ctx")
public final class ContextResolvingCmd {

    @Execute
    public void def(TestSource source, @ContextResolved PlayerData data) {
        Assertions.assertEquals(source.name(), data.name());
    }

    @SubCommand("sub")
    public void defSub(TestSource source, @ContextResolved Group group) {
        //throws an error
        System.out.println("DEFAULT SUBCMD EXECUTION, CONTEXT RESOLVED GROUP=" + group.name());
    }

}