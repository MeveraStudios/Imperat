package studio.mevera.imperat.verification;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
class SimpleVerifier<S extends Source> implements UsageVerifier<S> {

    SimpleVerifier() {
    }

    @Override
    public boolean verify(CommandUsage<S> usage) {
        if (usage.getParameters().isEmpty()) {
            return true;
        }

        int greedyCount = 0;
        for (int i = 0; i < usage.getMaxLength(); i++) {
            Argument<S> param = usage.loadCombinedParameters().get(i);
            if (param.isGreedy()) {
                greedyCount++;
            }
        }

        if (greedyCount > 1) {
            return false;
        }

        Argument<S> greedyParam = usage.getParameter(Argument::isGreedy);
        if (greedyParam == null) {
            return true;
        }

        //debug combined params
        return greedyParam.position() == usage.getMaxLength() - 1;
    }

    @Override
    public boolean areAmbiguous(CommandUsage<S> firstUsage, CommandUsage<S> secondUsage) {
        //check length
        boolean sameLength = firstUsage.getMinLength() == secondUsage.getMinLength();
        boolean hasSubCommands = firstUsage.hasParamType(Command.class)
                                         && secondUsage.hasParamType(Command.class);

        if (sameLength && hasSubCommands) {
            List<Argument<S>> parameterList1 = new ArrayList<>(firstUsage.getParameters());
            parameterList1.removeIf((param) -> !param.isCommand());

            List<Argument<S>> parameterList2 = new ArrayList<>(secondUsage.getParameters());
            parameterList2.removeIf((param) -> !param.isCommand());

            return parameterList1.equals(parameterList2);
        }

        if (sameLength) {
            final int capacity = firstUsage.getMinLength();
            for (int i = 0; i < capacity; i++) {
                Argument<S> firstUsageParameter = firstUsage.getParameter(i);
                Argument<S> secondUsageParameter = secondUsage.getParameter(i);
                if (firstUsageParameter == null || secondUsageParameter == null) {
                    break;
                }

                if ((firstUsageParameter.isCommand() && !secondUsageParameter.isCommand())
                            || (!firstUsageParameter.isCommand() && secondUsageParameter.isCommand())) {
                    return false;
                }
            }
        }

        return sameLength;
    }

}
