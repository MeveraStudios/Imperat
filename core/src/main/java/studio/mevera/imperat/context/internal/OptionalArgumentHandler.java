package studio.mevera.imperat.context.internal;

import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Patterns;

public final class OptionalArgumentHandler<S extends CommandSource> implements ArgumentHandler<S> {

    @Override
    public void handle(TreeExecutionResult<S> result, ExecutionContext<S> context, Cursor<S> cursor) throws CommandException {
        Argument<S> currentParameter = cursor.currentParameterIfPresent();
        String currentRaw = cursor.currentRawIfPresent();

        if (currentParameter == null || currentRaw == null) {
            return;
        }
        if (Patterns.isInputFlag(currentRaw)) {
            boolean containsAnyFlag = !context.getDetectedPathway().getFlagExtractor().getRegisteredFlags().isEmpty();
            if (containsAnyFlag) {
                cursor.skipRaw();
                var extracted = context.getDetectedPathway().getFlagExtractor().extract(Patterns.withoutFlagSign(currentRaw));
                boolean allTrueFlags = extracted.stream().noneMatch(FlagArgument::isSwitch);
                if (allTrueFlags) {
                    cursor.skipRaw();
                }
                return;
            }
        }
        if (!currentParameter.isOptional()) {
            return;
        }
        resolveOptional(currentRaw, currentParameter, context, cursor);
    }

    private void resolveOptional(
            String currentRaw,
            Argument<S> currentParameter,
            ExecutionContext<S> context,
            Cursor<S> stream
    ) throws CommandException {
        ImperatDebugger.debug("Handling " + currentParameter.format());
        // Step 1: Calculate obligation map for all remaining parameters
        boolean isObligatedToSkip = calculateObligationToSkip(stream);

        if (isObligatedToSkip) {
            ImperatDebugger.debug("MUST SKIP");
            // MUST skip - downstream required parameters need the inputs
            Object defaultValue = getDefaultValue(context, currentParameter);
            context.parseArgument(stream, defaultValue);
            stream.skipParameter();
            return;
        }

        // Step 2: Apply skipping logic based on configuration
        if (!context.imperatConfig().handleExecutionMiddleOptionalSkipping()) {
            // Strict positional order - MUST consume input
            ImperatDebugger.debug("MUST CONSUME INPUT, OPTION SKIPPING IS DISABLED");
            consumeInput(currentParameter, context, stream);
            return;
        }

        // Step 3: Smart skipping enabled - check type compatibility
        if (!Patterns.isInputFlag(currentRaw)) {
            boolean typeMatches = false;
            try {
                currentParameter.type().parse(context, currentParameter, currentRaw);
                typeMatches = true;
            } catch (Exception ignored) {
                // Not a match
            }
            if (typeMatches) {
                // Type matches - CAN consume input
                ImperatDebugger.debug("IT MATCHES TYPE, CONSUMING RIGHT AWAY");
                consumeInput(currentParameter, context, stream);
                return;
            }
        }

        // Step 4: Type doesn't match - check if downstream optional can handle it
        Argument<S> bestMatch = findBestDownstreamMatch(stream, context);

        if (bestMatch != null && !hasRequiredParametersBetween(stream, bestMatch)) {
            // Found better match downstream with no required parameters in between
            ImperatDebugger.debug("Found better match down stream : '" + bestMatch.format() + "'");
            Object defaultValue = getDefaultValue(context, currentParameter);
            context.parseArgument(stream, defaultValue);
            stream.skipParameter();
            return;
        }

        // Step 5: No better match found - try to force consume with fallback
        try {
            consumeInput(currentParameter, context, stream);
            ImperatDebugger.debug("CONSUMING....");
        } catch (CommandException e) {
            // Type parsing failed - fall back to default value
            ImperatDebugger.debug("FAILED TO CONSUME, SETTING DEFAULT");
            Object defaultValue = getDefaultValue(context, currentParameter);
            context.parseArgument(stream, defaultValue);
            stream.skipParameter();
        }
    }

    /**
     * Step 1: Calculate if current optional parameter is obligated to skip
     * to ensure downstream required parameters get satisfied
     */
    private boolean calculateObligationToSkip(Cursor<S> stream) {
        int currentParamPos = stream.currentParameterPosition();
        int currentRawPos = stream.currentRawPosition();

        // Count remaining required parameters after current position
        int remainingRequired = 0;
        for (int i = currentParamPos + 1; i < stream.parametersLength(); i++) {
            Argument<S> param = stream.getParametersList().get(i);
            if (param.isRequired()) {
                remainingRequired++;
            }
        }

        // Count remaining raw inputs after current position
        int remainingRawInputs = stream.rawsLength() - currentRawPos - 1;

        // If more required parameters than available inputs, must skip current optional
        return remainingRequired > remainingRawInputs;
    }

    /**
     * Step 4: Find the best downstream optional parameter that can handle current input
     */
    private Argument<S> findBestDownstreamMatch(Cursor<S> stream, ExecutionContext<S> ctx) {
        int currentParamPos = stream.currentParameterPosition();
        int currRawPos = stream.currentRawPosition();
        // Look for downstream optional parameters that match the input type
        for (int i = currentParamPos + 1; i < stream.parametersLength(); i++) {
            Argument<S> param = stream.getParametersList().get(i);
            if (param.isOptional() && !param.isFlag()) {
                String input = ctx.arguments().getOr(currRawPos, null);
                if (input != null) {
                    try {
                        param.type().parse(ctx, param, input);
                        return param;
                    } catch (Exception ignored) {
                        // Not a match
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if there are any required parameters between current position and target parameter
     */
    private boolean hasRequiredParametersBetween(Cursor<S> stream, Argument<S> targetParam) {
        int currentParamPos = stream.currentParameterPosition();
        int targetParamPos = targetParam.getPosition();

        for (int i = currentParamPos + 1; i < targetParamPos; i++) {
            Argument<S> param = stream.getParametersList().get(i);
            if (param.isRequired()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Consume the current raw input with the current parameter
     */
    private void consumeInput(
            Argument<S> currentParameter,
            ExecutionContext<S> context,
            Cursor<S> stream
    ) throws CommandException {
        ArgumentValueBinder.bindCurrentParameter(context, stream);
    }

    /**
     * Get the default value for an optional parameter
     */
    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ExecutionContext<S> context, Argument<S> argument) throws CommandException {
        DefaultValueProvider optionalSupplier = argument.getDefaultValueSupplier();
        if (optionalSupplier.isEmpty()) {
            return null;
        }
        String value = optionalSupplier.provide(context, argument);

        if (value != null) {
            return (T) argument.type().parse(context, argument, value);
        }

        return null;
    }
}
