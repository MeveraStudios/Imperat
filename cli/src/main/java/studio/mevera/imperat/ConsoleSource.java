package studio.mevera.imperat;

import studio.mevera.imperat.context.Source;

import java.io.PrintStream;

public class ConsoleSource implements Source {

    private final PrintStream outputStream;

    public ConsoleSource(final PrintStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public String name() {
        return "CONSOLE";
    }

    @Override
    public PrintStream origin() {
        return outputStream;
    }

    @Override
    public void reply(final String message) {
        outputStream.println(message);
    }

    @Override
    public void warn(final String message) {
        outputStream.println("[WARN] " + message);
    }

    @Override
    public void error(final String message) {
        outputStream.println("[ERROR] " + message);
    }

    @Override
    public boolean isConsole() {
        return true;
    }

}
