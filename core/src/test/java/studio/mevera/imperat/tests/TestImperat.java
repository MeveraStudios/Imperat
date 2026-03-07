package studio.mevera.imperat.tests;

import studio.mevera.imperat.BaseImperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;

import java.io.PrintStream;

public final class TestImperat extends BaseImperat<TestCommandSource> {

    TestImperat(ImperatConfig<TestCommandSource> config) {
        super(config);
    }

    @Override
    public TestCommandSource createDummySender() {
        return new TestCommandSource(System.out);
    }

    /**
     * Wraps the sender into a built-in command-sender valueType
     *
     * @param sender the sender's actual value
     * @return the wrapped command-sender valueType
     */
    @Override
    public TestCommandSource wrapSender(Object sender) {
        return new TestCommandSource((PrintStream) sender);
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
    public void registerSimpleCommand(Command<TestCommandSource> command) {
        super.registerSimpleCommand(command);
        command.visualizeTree();
    }
}