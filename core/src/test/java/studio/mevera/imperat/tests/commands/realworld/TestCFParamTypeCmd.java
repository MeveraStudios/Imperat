package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

import java.util.concurrent.CompletableFuture;

@Command("testcf")
public class TestCFParamTypeCmd {

    @Usage
    public void test(TestSource source, @Greedy CompletableFuture<String> future) {
        future.whenComplete(((s, throwable) -> source.reply(s)));
    }

}
