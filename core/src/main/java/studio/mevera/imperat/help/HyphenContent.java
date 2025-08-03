package studio.mevera.imperat.help;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;

public record HyphenContent<S extends Source>(Command<S> command, int currentPage, int maxPages) {
    public static <S extends Source> HyphenContent<S> of(Command<S> cmd, int currentPage, int maxPages) {
        return new HyphenContent<>(cmd, currentPage, maxPages);
    }
}
