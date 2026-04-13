package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.context.CommandSource;

@FunctionalInterface
public interface HelpSender<S extends CommandSource, O> {

    static <S extends CommandSource> HelpSender<S, String> forReplies() {
        return (source, message) -> source.reply(message);
    }

    void send(S source, O message);

    default void sendAll(S source, Iterable<? extends O> messages) {
        for (O message : messages) {
            send(source, message);
        }
    }
}
