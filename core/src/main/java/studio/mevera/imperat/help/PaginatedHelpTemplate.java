package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.NoHelpException;
import studio.mevera.imperat.exception.NoHelpPageException;
import studio.mevera.imperat.util.text.PaginatedText;
import studio.mevera.imperat.util.text.TextPage;

import java.util.Collection;

/**
 * Represents a help menu with pages
 */
@ApiStatus.AvailableSince("1.0.0")
public non-sealed abstract class PaginatedHelpTemplate<S extends Source> extends HelpTemplate<S> {

    protected final int syntaxesPerPage;

    public PaginatedHelpTemplate(
            UsageFormatter formatter,
            int syntaxesPerPage
    ) {
        super(formatter);
        this.syntaxesPerPage = syntaxesPerPage;
    }

    @Override
    public final void display(ExecutionContext<S> context, S source, UsageFormatter formatter, Collection<? extends CommandUsage<S>> commandUsages) throws ImperatException {
        //empty
        throw new UnsupportedOperationException("Display method isn't supported in PaginatedHelpTemplate");
    }

    @Override
    public void provide(ExecutionContext<S> context, S source) throws ImperatException {

        Command<S> command = context.command();
        var commandUsages = command.usages().stream().filter((usage)-> {
            if(usage.isDefault()) return false;
            if(usage.getParameters().isEmpty()) return false;
            var lastParam = usage.getParameters().get(usage.size()-1);
            if(lastParam.isCommand()) {
                Command<S> subCommand = lastParam.asCommand();
                //main usage is NOT default -> it has params, while this current usage which has the sub command param has no args, meaning
                // let's show the main usage only
                return subCommand.getMainUsage().isDefault();
            }
            return true;
        }).toList();

        final int maxUsages = commandUsages.size();
        if (maxUsages == 0) {
            throw new NoHelpException(context);
        }

        PaginatedText<CommandUsage<S>> paginatedText = new PaginatedText<>(syntaxesPerPage);

        commandUsages.forEach(paginatedText::add);
        paginatedText.paginate();

        int page = context.getArgumentOr("page", 1);
        TextPage<CommandUsage<S>> textPage = paginatedText.getPage(page);
        if (textPage == null) {
            throw new NoHelpPageException(context);
        }

        displayHeaderHyphen(command, source, page, paginatedText.getMaxPages());
        int index = 0;
        for (var usage : textPage.asList()) {
            source.reply(formatter.format(context.command(), usage, index));
            index++;
        }
        displayFooterHyphen(command, source, page, paginatedText.getMaxPages());
    }
}
