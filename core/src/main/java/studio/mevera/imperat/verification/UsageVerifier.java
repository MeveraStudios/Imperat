package studio.mevera.imperat.verification;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.Source;

/**
 * Verifies that the {@link CommandUsage} is suitable
 * to be added to the command
 *
 * <p>
 * Rule(s) recommended following when creating a usage:
 * <p>
 * 1) A usage MUST have AT LEAST ONE required parameter at the beginning before any other
 * parameters whether optional or required
 * <p>
 * 2) If the usage doesn't have any subcommands,
 * it must not be duplicated in a similar parameters pattern
 * <p>
 * 3) It MUSTN'T have a greedy argument in the middle of the parameters,
 * therefore, if you want to add any greedy arguments,
 * you should put A SINGLE (not multiple) greedy argument
 * at the END of the usage parameters list ONLY
 */
@ApiStatus.AvailableSince("1.0.0")
public interface UsageVerifier<S extends Source> {

    static <S extends Source> UsageVerifier<S> simpleVerifier() {
        return new SimpleVerifier<>();
    }

    static <S extends Source> UsageVerifier<S> typeTolerantVerifier() {
        return new TypeTolerantVerifier<>();
    }

    /**
     * @param usage the usage
     * @return Verifies the usage to be acceptable
     */
    boolean verify(CommandUsage<S> usage);

    /**
     * @param firstUsage  the first usage
     * @param secondUsage the second usage
     * @return whether both usages are similar
     * and/or share similar indistinguishable parameters or syntax
     */
    boolean areAmbiguous(CommandUsage<S> firstUsage, CommandUsage<S> secondUsage);
}
