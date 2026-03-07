package studio.mevera.imperat.command.processors.impl;

import studio.mevera.imperat.context.CommandSource;

public final class DefaultProcessors {

    public static <S extends CommandSource> CooldownProcessor<S> cooldownProcessor() {
        return new CooldownProcessor<>();
    }

}
