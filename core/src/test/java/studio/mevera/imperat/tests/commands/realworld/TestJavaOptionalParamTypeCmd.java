package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

import java.util.Optional;

@RootCommand("testoptional")
public class TestJavaOptionalParamTypeCmd {

    @Execute
    public void test(TestSource source, @Greedy Optional<String> text) {
        text.ifPresent(source::reply);
    }

}
