package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.NoHelpException;

/**
 * Represents a template for holding information that the help-writer will use
 * to display the help menu of a single command to the command sender
 */
@ApiStatus.AvailableSince("1.0.0")
public sealed abstract class HelpTemplate<S extends Source> implements HelpProvider<S> permits HelpTemplateImpl, PaginatedHelpTemplate {

    protected final UsageFormatter formatter;

    public HelpTemplate(UsageFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param command the command
     * @return the header
     */
    public abstract String getHeader(Command<S> command, int currentPage, int maxPages);

    /**
     * @param command the command
     * @return the footer
     */
    public abstract String getFooter(Command<S> command, int currentPage, int maxPages);


    @Override
    public void provide(ExecutionContext<S> context, S source) throws ImperatException {
        Command<S> command = context.command();

        final int maxUsages = command.usages().size();
        if (maxUsages == 0) {
            throw new NoHelpException();
        }
        int page = context.getArgumentOr("page", 1);
        displayHeaderHyphen(command, source, page, 1);
        display(context, source, formatter, command.usages());
        displayFooterHyphen(command, source, page, 1);
    }

    public abstract void displayHeaderHyphen(Command<S> command, Source source, int page, int maxPages);

    public abstract void displayFooterHyphen(Command<S> command, Source source, int page, int maxPages);

}
