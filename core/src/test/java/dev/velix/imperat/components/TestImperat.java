package dev.velix.imperat.components;

import dev.velix.imperat.BaseImperat;
import dev.velix.imperat.ImperatConfig;
import dev.velix.imperat.command.Command;

import java.io.PrintStream;

public final class TestImperat extends BaseImperat<TestSource> {

    TestImperat(ImperatConfig<TestSource> config) {
        super(config);
    }

    /**
     * Wraps the sender into a built-in command-sender valueType
     *
     * @param sender the sender's actual value
     * @return the wrapped command-sender valueType
     */
    @Override
    public TestSource wrapSender(Object sender) {
        return new TestSource((PrintStream) sender);
    }

    /**
     * @return the platform of the module
     */
    @Override
    public Object getPlatform() {
        return null;
    }


    /**
     *
     */
    @Override
    public void shutdownPlatform() {

    }


    @Override
    public void registerCommand(Command<TestSource> command) {
        super.registerCommand(command);
        command.visualizeTree();
    }
}
