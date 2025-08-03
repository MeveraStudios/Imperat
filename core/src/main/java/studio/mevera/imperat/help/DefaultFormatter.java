package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Source;

@ApiStatus.Internal
final class DefaultFormatter implements UsageFormatter {

    final static DefaultFormatter INSTANCE = new DefaultFormatter();

    private DefaultFormatter() {

    }

    @Override
    public <S extends Source> String format(Command<S> command, CommandUsage<S> usage, int index) {
        String format = "/" + CommandUsage.format(command, usage);
        return "&8&l[&3+&8]&r &a" + format + " &r&l-&r&e " + usage.description();
    }


}
