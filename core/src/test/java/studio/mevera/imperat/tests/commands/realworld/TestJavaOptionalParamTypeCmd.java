package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

import java.util.Optional;

@Command("testoptional")
public class TestJavaOptionalParamTypeCmd {

    @Usage
    public void test(TestSource source, @Greedy Optional<String> text) {
        text.ifPresent(source::reply);
    }

}
