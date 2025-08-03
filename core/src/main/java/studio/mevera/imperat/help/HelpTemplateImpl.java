package studio.mevera.imperat.help;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;

import java.util.Collection;

final class HelpTemplateImpl<S extends Source> extends HelpTemplate<S> {

    private final HelpHyphen<S> headerProvider, footerProvider;
    private final @Nullable UsageDisplayer<S> displayerFunc;

    HelpTemplateImpl(
        UsageFormatter formatter,
        HelpHyphen<S> headerProvider,
        HelpHyphen<S> footerProvider,
        @Nullable UsageDisplayer<S> displayerFunc
    ) {
        super(formatter);
        this.headerProvider = headerProvider;
        this.footerProvider = footerProvider;
        this.displayerFunc = displayerFunc;
    }

    @Override
    public String getHeader(Command<S> command, int currentPage, int maxPages) {
        return headerProvider.value(HyphenContent.of(command, currentPage, maxPages));
    }

    @Override
    public String getFooter(Command<S> command, int currentPage, int maxPages) {
        return footerProvider.value(HyphenContent.of(command, currentPage, maxPages));
    }

    @Override
    public void displayHeaderHyphen(Command<S> command, Source source, int page, int maxPages) {
        source.reply(getHeader(command, 1, 1));
    }

    @Override
    public void displayFooterHyphen(Command<S> command, Source source, int page, int maxPages) {
        source.reply(getFooter(command, 1, 1));
    }


    @Override
    public void display(ExecutionContext<S> context, S source, UsageFormatter formatter, Collection<? extends CommandUsage<S>> commandUsages) throws ImperatException {
        if (displayerFunc == null)
            super.display(context, source, formatter, commandUsages);
        else
            displayerFunc.accept(context, source, commandUsages);
    }
}
