package studio.mevera.imperat;

import java.io.*;

public final class CommandLineImperat extends BaseImperat<ConsoleSource> {

    private InputStream input;

    /**
     * Creates a new CommandLineConfigBuilder instance.
     *
     * @param inputStream the input stream for command line input
     * @return a new CommandLineConfigBuilder instance
     */
    public static CommandLineConfigBuilder builder(InputStream inputStream) {
        return new CommandLineConfigBuilder(inputStream);
    }

    CommandLineImperat(InputStream inputStream, ImperatConfig<ConsoleSource> config) {
        super(config);
        this.input = inputStream;
    }

    @Override
    public InputStream getPlatform() {
        return input;
    }

    @Override
    public void shutdownPlatform() {
        input = null;
        throw new RuntimeException();
    }


    @Override
    public ConsoleSource wrapSender(Object sender) {
        if (!(sender instanceof PrintStream printStream)) {
            throw new UnsupportedOperationException("Sender must be a print stream");
        }
        return new ConsoleSource(printStream);
    }

    /**
     * Dispatches the command-line from the input stream provided
     *
     * @param outputStream the output stream/command-source origin
     */
    public void dispatch(OutputStream outputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line = reader.readLine();
            ConsoleSource prompt = wrapSender(outputStream);
            super.executeSafely(prompt, line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dispatches the command-line from the input stream provided
     * while using {@link System#out} as an {@link OutputStream}
     *
     */
    public void dispatch() {
        dispatch(System.out);
    }

}
