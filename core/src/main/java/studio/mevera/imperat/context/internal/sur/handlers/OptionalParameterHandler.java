package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.Patterns;

public final class OptionalParameterHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) throws CommandException {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();

        if (currentParameter == null || currentRaw == null) {
            return HandleResult.TERMINATE;
        }
        if(Patterns.isInputFlag(currentRaw)) {
            boolean containsAnyFlag = !context.getDetectedUsage().getFlagExtractor().getRegisteredFlags().isEmpty();
            if(containsAnyFlag) {
                stream.skipRaw();
                var extracted = context.getDetectedUsage().getFlagExtractor().extract(Patterns.withoutFlagSign(currentRaw));
                boolean allTrueFlags = extracted.stream().noneMatch(FlagParameter::isSwitch);
                if(allTrueFlags) {
                    stream.skipRaw();
                }
                return HandleResult.NEXT_HANDLER;
            }
        }
        if(!currentParameter.isOptional()) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            resolveOptional(currentRaw, currentParameter, context, stream);
            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }
    
    private void resolveOptional(
            String currentRaw,
            CommandParameter<S> currentParameter,
            ExecutionContext<S> context,
            CommandInputStream<S> stream
    ) throws CommandException {
        ImperatDebugger.debug("Handling " + currentParameter.format());
        // Step 1: Calculate obligation map for all remaining parameters
        boolean isObligatedToSkip = calculateObligationToSkip(stream);
        
        if (isObligatedToSkip) {
            ImperatDebugger.debug("MUST SKIP");
            // MUST skip - downstream required parameters need the inputs
            Object defaultValue = getDefaultValue(context, stream, currentParameter);
            context.resolveArgument(stream, defaultValue);
            stream.skipParameter();
            return;
        }
        
        // Step 2: Apply skipping logic based on configuration
        if (!context.imperatConfig().handleExecutionMiddleOptionalSkipping()) {
            // Strict positional order - MUST consume input
            ImperatDebugger.debug("MUST CONSUME INPUT, OPTION SKIPPING IS DISABLED");
            consumeInput(currentRaw, currentParameter, context, stream);
            return;
        }
        
        // Step 3: Smart skipping enabled - check type compatibility
        if (!Patterns.isInputFlag(currentRaw) && currentParameter.type().matchesInput(stream.currentRawPosition(), context, currentParameter)) {
            // Type matches - CAN consume input
            ImperatDebugger.debug("IT MATCHES TYPE, CONSUMING RIGHT AWAY");
            consumeInput(currentRaw, currentParameter, context, stream);
            return;
        }
        
        // Step 4: Type doesn't match - check if downstream optional can handle it
        CommandParameter<S> bestMatch = findBestDownstreamMatch(stream, context);
        
        if (bestMatch != null && !hasRequiredParametersBetween(stream, bestMatch)) {
            // Found better match downstream with no required parameters in between
            ImperatDebugger.debug("Found better match down stream : '" + bestMatch.format() + "'");
            Object defaultValue = getDefaultValue(context, stream, currentParameter);
            context.resolveArgument(stream, defaultValue);
            stream.skipParameter();
            return;
        }
        
        // Step 5: No better match found - try to force consume with fallback
        try {
            consumeInput(currentRaw, currentParameter, context, stream);
            ImperatDebugger.debug("CONSUMING....");
        } catch (CommandException e) {
            // Type parsing failed - fall back to default value
            ImperatDebugger.debug("FAILED TO CONSUME, SETTING DEFAULT");
            Object defaultValue = getDefaultValue(context, stream, currentParameter);
            context.resolveArgument(stream, defaultValue);
            stream.skipParameter();
        }
    }
    
    /**
     * Step 1: Calculate if current optional parameter is obligated to skip
     * to ensure downstream required parameters get satisfied
     */
    private boolean calculateObligationToSkip(CommandInputStream<S> stream) {
        int currentParamPos = stream.currentParameterPosition();
        int currentRawPos = stream.currentRawPosition();
        
        // Count remaining required parameters after current position
        int remainingRequired = 0;
        for (int i = currentParamPos + 1; i < stream.parametersLength(); i++) {
            CommandParameter<S> param = stream.getParametersList().get(i);
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
    private CommandParameter<S> findBestDownstreamMatch(CommandInputStream<S> stream, ExecutionContext<S> ctx) {
        int currentParamPos = stream.currentParameterPosition();
        int currRawPos = stream.currentRawPosition();
        // Look for downstream optional parameters that match the input type
        for (int i = currentParamPos + 1; i < stream.parametersLength(); i++) {
            CommandParameter<S> param = stream.getParametersList().get(i);
            if (param.isOptional() && !param.isFlag() &&
                    param.type().matchesInput(currRawPos, ctx, param)) {
                return param;
            }
        }
        
        return null;
    }
    
    /**
     * Check if there are any required parameters between current position and target parameter
     */
    private boolean hasRequiredParametersBetween(CommandInputStream<S> stream, CommandParameter<S> targetParam) {
        int currentParamPos = stream.currentParameterPosition();
        int targetParamPos = targetParam.position();
        
        for (int i = currentParamPos + 1; i < targetParamPos; i++) {
            CommandParameter<S> param = stream.getParametersList().get(i);
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
            String currentRaw,
            CommandParameter<S> currentParameter,
            ExecutionContext<S> context,
            CommandInputStream<S> stream
    ) throws CommandException {
        Object value = currentParameter.type().resolve(context, stream, currentRaw);
        context.resolveArgument(stream, value);
        stream.skip();
    }
    
    /**
     * Get the default value for an optional parameter
     */
    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ExecutionContext<S> context, CommandInputStream<S> stream, CommandParameter<S> parameter) throws CommandException {
        OptionalValueSupplier optionalSupplier = parameter.getDefaultValueSupplier();
        if (optionalSupplier.isEmpty()) {
            return null;
        }
        String value = optionalSupplier.supply(context, parameter);
        
        if (value != null) {
            return (T) parameter.type().resolve(context, stream, value);
        }
        
        return null;
    }
}