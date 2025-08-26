package studio.mevera.imperat.adventure;

import net.kyori.adventure.audience.Audience;

public abstract class CastingAdventure<S> implements AdventureProvider<S> {

    @Override
    public final Audience audience(final S sender) {
        return (Audience) sender;
    }
    
    
}
