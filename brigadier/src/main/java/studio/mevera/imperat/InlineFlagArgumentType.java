package studio.mevera.imperat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import studio.mevera.imperat.util.Patterns;

/**
 * Brigadier argument type that consumes a single whitespace-bounded token
 * and accepts it iff the token is an inline-flag form
 * ({@code -name=value} / {@code --name=value} / {@code -name} / {@code --name}).
 *
 * <p>Brigadier's stock {@link com.mojang.brigadier.arguments.StringArgumentType
 * StringArgumentType.string()/word()} halts at {@code =} because the
 * unquoted-string charset excludes it — leaves {@code =value} orphaned and
 * the client renders the token red. Registering a sibling {@code <flag>}
 * argument with this type lets the parse succeed (green) when the token
 * looks like a flag, and fall through to other siblings (positional /
 * literal) otherwise.</p>
 */
public final class InlineFlagArgumentType implements ArgumentType<String> {

    private static final SimpleCommandExceptionType NOT_INLINE_FLAG =
            new SimpleCommandExceptionType(() -> "Token is not an inline flag form");

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String token = reader.getString().substring(start, reader.getCursor());
        // Restrict to inline `=`-form only. Bare `-name` / `--name` falls
        // through to the LiteralCommandNode siblings registered for each
        // flag, preserving Brigadier's literal-first highlighting for the
        // space-separated form.
        if (token.isEmpty() || token.indexOf('=') < 0 || !Patterns.isInputFlag(token)) {
            // Revert cursor so subsequent siblings can re-attempt parsing the
            // same token. Without this Brigadier would treat the token as
            // partially consumed and dispatch falls into a broken state.
            reader.setCursor(start);
            throw NOT_INLINE_FLAG.createWithContext(reader);
        }
        return token;
    }
}
