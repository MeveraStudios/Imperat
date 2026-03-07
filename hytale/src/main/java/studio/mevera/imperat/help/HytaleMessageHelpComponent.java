package studio.mevera.imperat.help;

import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.HytaleCommandSource;
import studio.mevera.imperat.command.tree.help.theme.HelpComponent;

public class HytaleMessageHelpComponent extends HelpComponent<HytaleCommandSource, Message> {


    protected HytaleMessageHelpComponent(Message message) {
        super(message);
    }

    public static HytaleMessageHelpComponent of(Message message) {
        return new HytaleMessageHelpComponent(message);
    }


    @Override
    public void send(HytaleCommandSource source) {
        source.origin().sendMessage(componentValue);
    }

    @Override
    public @NotNull HelpComponent<HytaleCommandSource, Message> append(Message other) {
        this.componentValue = componentValue.insert(other);
        return this;
    }

    @Override
    public HelpComponent<HytaleCommandSource, Message> appendText(String text) {
        this.componentValue = componentValue.insert(text);
        return this;
    }

    @Override
    public @NotNull HelpComponent<HytaleCommandSource, Message> repeat(int times) {
        for (int i = 0; i < times; i++) {
            this.componentValue = componentValue.insert(componentValue);
        }
        return this;
    }
}
