package studio.mevera.imperat.bukkit.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple unit test for executing Imperat commands on Bukkit using MockBukkit.
 */
class BukkitImperatTest {

    private ServerMock server;
    private TestImperatPlugin plugin;
    private BukkitImperat imperat;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestImperatPlugin.class);
        imperat = plugin.getImperat();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCommandRegistered() {
        // Verify the "greet" command was registered in Imperat
        assertNotNull(imperat.getCommand("tell"), "Command 'greet' should be registered");
    }

    @Test
    void testDefCommandExecution() {
        // Create a mock player and execute the command
        String[] aliases = {"tell", "msg", "t", "w", "whisper", "pm"};

        PlayerMock player = server.addPlayer("TestPlayer");
        BukkitSource source = imperat.wrapSender(player);

        ImperatDebugger.setEnabled(true);
        for (String alias : aliases) {
            // Execute the command with arguments
            var res = imperat.execute(source, alias);

            // Verify the player received the correct message
            Assertions.assertEquals("Invalid command usage '/" + alias + "', you probably meant '/" + alias + " <name> <message...>'",
                    player.nextMessage());
        }

    }

    @Test
    void testArgumentSuggestionForCommandAliases() throws Exception {
        String[] aliases = {"tell", "msg", "t", "w", "whisper", "pm"};
        for (String alias : aliases) {
            // Verify that the "name" argument suggestions are provided for all aliases
            PlayerMock player = server.addPlayer("TestPlayer");
            BukkitSource source = imperat.wrapSender(player);

            var results = imperat.autoComplete(source, alias + " ")
                                     .join();
            Assertions.assertLinesMatch(List.of("Mazen", "Ahmed", "Eyad"), results, "Failed to get correct imperat-suggestions for alias: " + alias);


            var event = new AsyncTabCompleteEvent(
                    player,
                    new ArrayList<>(),
                    "/" + alias + " ",
                    true,
                    player.getLocation()
            );

            var thread = new Thread(() -> server.getPluginManager().callEvent(event));
            thread.start();
            thread.join();

            List<String> bukkitResults = event.getCompletions();
            Assertions.assertLinesMatch(List.of("Mazen", "Ahmed", "Eyad"), bukkitResults,
                    "Failed to get correct Bukkit-suggestions for alias: " + alias);
        }
    }
    //for paper

}



