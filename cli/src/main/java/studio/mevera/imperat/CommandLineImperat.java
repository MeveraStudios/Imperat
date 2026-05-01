package studio.mevera.imperat;

import studio.mevera.imperat.providers.CommandSourceMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public final class CommandLineImperat<S extends ConsoleCommandSource> extends BaseImperat<S> {

    private InputStream input;

    CommandLineImperat(InputStream inputStream, ImperatConfig<S> config) {
        super(config);
        this.input = inputStream;
    }

    public static CommandLineConfigBuilder<ConsoleCommandSource> builder(InputStream inputStream) {
        return new CommandLineConfigBuilder<>(inputStream, ConsoleCommandSource.class, CommandSourceMapper.identity());
    }

    public static <S extends ConsoleCommandSource> CommandLineConfigBuilder<S> builder(
            InputStream inputStream, Class<S> sourceClass, CommandSourceMapper<ConsoleCommandSource, S> mapper
    ) {
        return new CommandLineConfigBuilder<>(inputStream, sourceClass, mapper);
    }

    @Override
    public InputStream getPlatform() {
        return input;
    }

    @Override
    public void shutdownPlatform() {
        input = null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        ConsoleCommandSource platform = new ConsoleCommandSource(ConsoleLogger.SYSTEM);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        ConsoleCommandSource platform = new ConsoleCommandSource((ConsoleLogger) sender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    public void execute(ConsoleLogger consoleLogger) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line = reader.readLine();
            S prompt = wrapSender(consoleLogger);
            super.execute(prompt, line);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public void execute(OutputStream outputStream) {
        execute(ConsoleLogger.SYSTEM);
    }

    public void execute() {
        execute(ConsoleLogger.SYSTEM);
    }
}
