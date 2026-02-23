package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.Command;

/**
 * Represents the processes context of a command
 * entered by {@link Source}
 *
 * @param <S> the command sender valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public interface Context<S extends Source> {

    /**
     * @return imperat's instance
     */
    Imperat<S> imperat();

    /**
     * @return the config for imperat
     */
    ImperatConfig<S> imperatConfig();

    /**
     * @return The {@link Command} owning this context.
     */
    @NotNull Command<S> command();

    /**
     * @return the {@link Source} of the command
     * @see Source
     */
    @NotNull S source();

    /**
     * @return the root command entered by the {@link Source}
     */
    @NotNull
    String getRootCommandLabelUsed();

    /**
     * @return the arguments entered by the {@link Source}
     * @see ArgumentInput
     */
    ArgumentInput arguments();


}
