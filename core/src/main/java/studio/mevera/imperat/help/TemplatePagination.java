package studio.mevera.imperat.help;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

final class TemplatePagination<S extends Source> extends PaginatedHelpTemplate<S> {
    private final HelpTemplate<S> template;

    TemplatePagination(
        HelpTemplate<S> template,
        int syntaxesPerPage
    ) {
        super(template.formatter, syntaxesPerPage);
        this.template = template;
    }


    @Override
    public String getHeader(Command<S> command, int currentPage, int maxPages) {
        return template.getHeader(command, currentPage, maxPages);
    }

    @Override
    public String getFooter(Command<S> command, int currentPage, int maxPages) {
        return template.getFooter(command, currentPage, maxPages);
    }

    @Override
    public void displayHeaderHyphen(Command<S> command, Source source, int page, int maxPages) {
        source.reply(getHeader(command, page, maxPages));
    }

    @Override
    public void displayFooterHyphen(Command<S> command, Source source, int page, int maxPages) {
        source.reply(getHeader(command, page, maxPages));
    }
}