package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.util.Patterns;

import java.util.Optional;

public final class FreeFlagHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) {
        var lastParam = context.getDetectedUsage().getParameter(context.getDetectedUsage().size() - 1);
        
        String currentRaw;
        while ((currentRaw = stream.currentRawIfPresent()) != null) {
            if (lastParam != null && lastParam.isGreedy()) {
                break;
            }

            Optional<FlagData<S>> freeFlagData = context.getLastUsedCommand().getFlagFromRaw(currentRaw);
            if (Patterns.isInputFlag(currentRaw) && freeFlagData.isPresent()) {
                try {
                    FlagData<S> freeFlag = freeFlagData.get();
                    var value = ParameterTypes.flag(freeFlag).resolveFreeFlag(context, stream, freeFlag);
                    context.resolveFlag(value);
                } catch (ImperatException e) {
                    return HandleResult.failure(e);
                }
            }
            stream.skipRaw();
        }
        
        return HandleResult.TERMINATE;
    }
}