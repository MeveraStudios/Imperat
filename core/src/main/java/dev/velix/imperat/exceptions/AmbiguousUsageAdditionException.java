package dev.velix.imperat.exceptions;

import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.context.Source;

public final class AmbiguousUsageAdditionException extends RuntimeException {

    public <S extends Source> AmbiguousUsageAdditionException(
            final Command<S> command,
            final CommandUsage<S> first,
            final CommandUsage<S> second
    ) {
        super(
                String.format(
                        "Failed to add usage '%s' because it's ambiguous along with other usage '%s'",
                        CommandUsage.format(command, first),
                        CommandUsage.format(command, second)
                )
        );
    }


}
