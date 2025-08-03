package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Source;

/**
 * Represents a class responsible for formatting
 * each single {@link CommandUsage}
 */
@ApiStatus.AvailableSince("1.0.0")
public interface UsageFormatter {

    /**
     * Displays the usage by converting it into
     * an adventure component
     *
     * @param command the command
     * @param usage   the usage to display
     * @param index   the index of the usage
     * @param <S>     the sender-valueType
     * @return the usage component
     */
    <S extends Source> String format(
        Command<S> command,
        CommandUsage<S> usage,
        int index
    );

}
