package studio.mevera.imperat.verification;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;

/**
 * Represents a usage verifier where it checks for ambiguity in a slightly
 * different way than the {@link SimpleVerifier}.
 * Example: If you have two usages of a command: `/command <arg1>` and `/command <arg2>`,
 * while arg1 is a parameter of valueType String, and arg2 is a parameter of valueType Boolean,
 * It will not consider this an ambiguity unless both parameters are of same {@link Type} !
 *
 * @param <S> the command sender valueType
 */
@ApiStatus.Internal
final class TypeTolerantVerifier<S extends Source> extends SimpleVerifier<S> {

    @Override
    public boolean verify(CommandUsage<S> usage) {
        return super.verify(usage); //I just like the super here lol
    }

    @Override
    public boolean areAmbiguous(CommandUsage<S> firstUsage, CommandUsage<S> secondUsage) {
        int sizeDiff = firstUsage.getMinLength() - secondUsage.getMinLength();
        if (sizeDiff != 0) {
            return false;
        }

        int capacity = firstUsage.getMinLength();
        boolean noDiff = true;
        for (int depth = 0; depth < capacity; depth++) {
            var param1 = firstUsage.getParameter(depth);
            var param2 = secondUsage.getParameter(depth);
            assert param1 != null && param2 != null;

            if (areSimilarParameters(param1, param2)) {
                continue;
            }
            noDiff = false;
            break;
        }

        return noDiff;
    }

    private boolean areSimilarParameters(Argument<S> param1, Argument<S> param2) {
        if (!param1.isCommand() && !param2.isCommand()) {
            return TypeUtility.matches(param1.valueType(), param2.valueType());
        } else if (param1.isCommand() && param2.isCommand()) {
            return param1.equals(param2);
        }
        return false;
    }

}
