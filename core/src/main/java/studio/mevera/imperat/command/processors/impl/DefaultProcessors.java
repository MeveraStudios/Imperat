package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.context.Source;

public final class DefaultProcessors {

    public static <S extends Source> CooldownProcessor<S> cooldownProcessor() {
        return new CooldownProcessor<>();
    }

}
