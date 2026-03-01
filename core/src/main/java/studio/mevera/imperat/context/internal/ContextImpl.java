package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;

class ContextImpl<S extends Source> implements CommandContext<S> {

    protected final Imperat<S> imperat;
    protected final ImperatConfig<S> imperatConfig;

    private final Command<S> commandUsed;
    private final S source;
    private final String label;
    private final ArgumentInput raw;

    public ContextImpl(Imperat<S> imperat, Command<S> commandUsed, S source, String label, ArgumentInput raw) {
        this.imperat = imperat;
        this.imperatConfig = imperat.config();
        this.commandUsed = commandUsed;
        this.source = source;
        this.label = label;
        this.raw = raw;
    }

    @Override
    public Imperat<S> imperat() {
        return imperat;
    }

    @Override
    public ImperatConfig<S> imperatConfig() {
        return imperatConfig;
    }


    @Override
    public @NotNull Command<S> command() {
        return commandUsed;
    }

    @Override
    public @NotNull S source() {
        return source;
    }

    /**
     * @return the root command entered by the {@link Source}
     */
    @Override
    public @NotNull String getRootCommandLabelUsed() {
        return label;
    }

    @Override
    public @NotNull ArgumentInput arguments() {
        return raw;
    }


    public Command<S> getCommandUsed() {
        return this.commandUsed;
    }

    public S getSource() {
        return this.source;
    }

    public ArgumentInput getRaw() {
        return this.raw;
    }

}
