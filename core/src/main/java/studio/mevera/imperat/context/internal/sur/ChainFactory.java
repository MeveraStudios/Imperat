package studio.mevera.imperat.context.internal.sur;

import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.sur.handlers.EmptyInputHandler;
import studio.mevera.imperat.context.internal.sur.handlers.FlagInputHandler;
import studio.mevera.imperat.context.internal.sur.handlers.NonFlagWhenExpectingFlagHandler;
import studio.mevera.imperat.context.internal.sur.handlers.OptionalParameterHandler;
import studio.mevera.imperat.context.internal.sur.handlers.ParameterHandler;
import studio.mevera.imperat.context.internal.sur.handlers.RequiredParameterHandler;
import studio.mevera.imperat.context.internal.sur.handlers.SubCommandHandler;

import java.util.ArrayList;
import java.util.List;

public class ChainFactory {
    
    public static <S extends Source> ParameterChain<S> createDefaultChain() {
        return ChainFactory.<S>builder()
            .withHandler(new EmptyInputHandler<>())
            .withHandler(new SubCommandHandler<>())
            .withHandler(new FlagInputHandler<>())
            .withHandler(new NonFlagWhenExpectingFlagHandler<>())
            .withHandler(new RequiredParameterHandler<>())
            .withHandler(new OptionalParameterHandler<>())
            .build();
    }
    
    public static <S extends Source> ChainBuilder<S> builder() {
        return new ChainBuilder<>();
    }
    
    public static class ChainBuilder<S extends Source> {
        private final List<ParameterHandler<S>> handlers = new ArrayList<>();
        
        public ChainBuilder<S> withHandler(ParameterHandler<S> handler) {
            handlers.add(handler);
            return this;
        }
        
        public ParameterChain<S> build() {
            return new ParameterChain<>(handlers);
        }
    }
}