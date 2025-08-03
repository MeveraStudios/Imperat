package studio.mevera.imperat.help;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.NoHelpException;
import studio.mevera.imperat.exception.NoHelpPageException;

import java.util.Collection;

//TODO add per-command help provider
/**
 * A class that whose only purpose is to
 * provide/send a help message to the {@link Source}
 * through the command's {@link Context}
 *
 * @param <S> the valueType of command source
 */
@ApiStatus.AvailableSince("1.0.0")
public interface HelpProvider<S extends Source> {

    /**
     * Provides the {@link Source} the help menu using {@link ExecutionContext}
     * <p>
     * Displays each {@link CommandUsage} formatted by {@link UsageFormatter} to the source
     * through the method {@link HelpProvider#display(ExecutionContext, Source, UsageFormatter, Collection)}
     * <p>
     * if command usages are empty/no command usages, it will throw {@link NoHelpException}
     * <p>
     * If used with the instance of a {@link PaginatedHelpTemplate} :
     * <ul>
     *     <li>while having no pages for {@link CommandUsage} it will also throw {@link NoHelpException}.</li>
     *     <li>while providing invalid page from {@link ExecutionContext}, it will throw {@link NoHelpPageException}</li>
     * </ul>
     * </p>
     *
     * @param context the command's context
     * @param source the source
     * @throws ImperatException if any of the above criteria is met.
     */
    void provide(ExecutionContext<S> context, S source) throws ImperatException;

    default void display(ExecutionContext<S> context, S source, UsageFormatter formatter, Collection<? extends CommandUsage<S>> usages) throws ImperatException {
        int index = 0;
        for (CommandUsage<S> usage : usages) {
            source.reply(formatter.format(context.command(), usage, index));
            index++;
        }
    }

    static <S extends Source> NormalBuilder<S> template() {
        return new NormalBuilder<>();
    }

    static <S extends Source> PaginationBuilder<S> paginated(int usagesPerPage) {
        return new PaginationBuilder<>(usagesPerPage);
    }

    @SuppressWarnings("unchecked")
    abstract class Builder<S extends Source, T extends HelpProvider<S>, B extends Builder<S, T, B>> {

        protected UsageFormatter formatter = DefaultFormatter.INSTANCE;
        protected HelpHyphen<S> headerProvider, footerProvider;
        protected UsageDisplayer<S> displayer;

        protected Builder() {

        }

        public @NotNull B formatter(UsageFormatter formatter) {
            this.formatter = formatter;
            return (B) this;
        }

        public @NotNull B header(HelpHyphen<S> headerProvider) {
            this.headerProvider = headerProvider;
            return (B) this;
        }

        public @NotNull B footer(HelpHyphen<S> footerProvider) {
            this.footerProvider = footerProvider;
            return (B) this;
        }

        public @NotNull B displayer(UsageDisplayer<S> displayer) {
            this.displayer = displayer;
            return (B) this;
        }

        protected abstract T createInstance();

        public T build() {
            if (formatter == null) {
                return null;
            }
            return createInstance();
        }
    }

    final class NormalBuilder<S extends Source> extends Builder<S, HelpTemplate<S>, NormalBuilder<S>> {
        NormalBuilder() {
        }

        @Override
        protected HelpTemplate<S> createInstance() {
            return new HelpTemplateImpl<>(formatter, headerProvider, footerProvider, displayer);
        }
    }

    final class PaginationBuilder<S extends Source> extends Builder<S, HelpTemplate<S>, PaginationBuilder<S>> {
        private final int syntaxesPerPage;

        PaginationBuilder(int syntaxesPerPage) {
            this.syntaxesPerPage = syntaxesPerPage;
        }

        @Override
        protected HelpTemplate<S> createInstance() {
            return new TemplatePagination<>(
                new HelpTemplateImpl<>(formatter, headerProvider, footerProvider, displayer),
                syntaxesPerPage
            );
        }
    }


}
