package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

import java.util.concurrent.CompletableFuture;

@RootCommand("testcf")
public class TestCFParamTypeCmd {

    @Execute
    public void test(TestSource source, @Greedy CompletableFuture<String> future) {
        future.whenComplete(((s, throwable) -> source.reply(s)));
    }

}
