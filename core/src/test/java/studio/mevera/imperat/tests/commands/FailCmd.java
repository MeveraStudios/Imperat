package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Context;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.errors.CustomException;

@RootCommand("fail")
public class FailCmd {

    @Execute
    public void t(TestSource src, @Context CommandContext<TestSource> ctx) throws CustomException {
        throw new CustomException();
    }
}
