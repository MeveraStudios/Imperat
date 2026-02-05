package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.errors.CustomException;

@Command("fail")
public class FailingCmd {

    @Execute
    public void t(TestSource src, @ContextResolved Context<TestSource> ctx) throws CustomException {
        throw new CustomException();
    }
}
