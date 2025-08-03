package studio.mevera.imperat.context.internal.sur;

import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.sur.handlers.*;

import java.util.ArrayList;
import java.util.List;

public class ChainFactory {
    
    public static <S extends Source> ParameterChain<S> createDefaultChain() {
        return ChainFactory.<S>builder()
            .withHandler(new EmptyInputHandler<>())
            .withHandler(new CommandParameterHandler<>())
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