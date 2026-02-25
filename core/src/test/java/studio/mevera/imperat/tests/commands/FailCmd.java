package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.errors.CustomException;

@RootCommand("fail")
public class FailCmd {

    @Execute
    public void t(TestSource src, @ContextResolved Context<TestSource> ctx) throws CustomException {
        throw new CustomException();
    }
}
