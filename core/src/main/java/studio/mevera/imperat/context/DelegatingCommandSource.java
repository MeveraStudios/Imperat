package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Convenience base class for plugin-defined custom source types that
 * forward the standard {@link CommandSource} methods to a held platform
 * source {@code P}. Subclass to add domain-specific methods (audit
 * fields, locale handles, transient metadata, etc.) without rewriting
 * the boilerplate every {@code CommandSource} requires.
 *
 * <p>Used in conjunction with the v4 custom-source SPI: a plugin that
 * wants their own source type to be canonical declares
 * {@code class MyServerSource extends DelegatingCommandSource<BukkitCommandSource>}
 * and supplies a {@code CommandSourceMapper<BukkitCommandSource, MyServerSource>}
 * to the builder.</p>
 *
 * <p><b>Why not just extend the platform source directly?</b> On platforms
 * where the platform source is a final or sealed class (or a record), or
 * when the plugin author wants their custom source to be backed by a
 * field rather than inheritance, this delegating shape is the cleaner
 * option. When the platform source IS extensible (as is the case for
 * {@code BukkitCommandSource}), direct subclassing remains an equally
 * valid path — both work with the mapper SPI.</p>
 *
 * @param <P> the held platform source type
 *
 * @since 4.0.0
 */
public abstract class DelegatingCommandSource<P extends CommandSource> implements CommandSource {

    private final P platform;

    protected DelegatingCommandSource(@NotNull P platform) {
        this.platform = platform;
    }

    /**
     * Direct accessor for the held platform source. Subclasses can
     * expose this through a domain-named alias if they prefer (e.g.
     * {@code public BukkitCommandSource bukkit()}).
     */
    public @NotNull P platform() {
        return platform;
    }

    @Override
    public String name() {
        return platform.name();
    }

    @Override
    public Object origin() {
        return platform.origin();
    }

    @Override
    public void reply(String message) {
        platform.reply(message);
    }

    @Override
    public void warn(String message) {
        platform.warn(message);
    }

    @Override
    public void error(String message) {
        platform.error(message);
    }

    @Override
    public boolean isConsole() {
        return platform.isConsole();
    }

    @Override
    public UUID uuid() {
        return platform.uuid();
    }

    @Override
    public <T> T as(Class<T> clazz) {
        return platform.as(clazz);
    }
}
