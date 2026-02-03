package studio.mevera.imperat.tests;

import studio.mevera.imperat.BaseImperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;

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
    public void registerSimpleCommand(Command<TestSource> command) {
        super.registerSimpleCommand(command);
        command.visualizeTree();
    }
}