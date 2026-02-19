package studio.mevera.imperat.bukkit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import studio.mevera.imperat.context.ExecutionResult;

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
        assertNotNull(imperat.getCommand("greet"), "Command 'greet' should be registered");
    }

    @Test
    void testGreetDefaultExecution() {
        // Execute the command with no arguments via Imperat's API
        PlayerMock player = server.addPlayer("TestPlayer");
        BukkitSource source = imperat.wrapSender(player);

        ExecutionResult<BukkitSource> result = imperat.execute(source, "greet");

        assertFalse(result.hasFailed(), "Default greet execution should succeed");
        // The command should have sent "Hello, World!" to the player
        assertEquals("Hello, World!", player.nextMessage());
    }

    @Test
    void testGreetWithNameExecution() {
        // Execute: /greet Steve
        PlayerMock player = server.addPlayer("TestPlayer");
        BukkitSource source = imperat.wrapSender(player);

        ExecutionResult<BukkitSource> result = imperat.execute(source, "greet Steve");

        assertFalse(result.hasFailed(), "Greet with name should succeed");
        assertEquals("Hello, Steve!", player.nextMessage());
    }

    @Test
    void testConsoleExecution() {
        // Execute command from console via Imperat's API
        BukkitSource consoleSource = imperat.wrapSender(server.getConsoleSender());

        ExecutionResult<BukkitSource> result = imperat.execute(consoleSource, "greet");

        assertFalse(result.hasFailed(), "Console greet execution should succeed");
    }

    @Test
    void testGreetArgumentSuggestion() throws Exception {
        // Test that suggestions for the "name" argument are provided
        PlayerMock player = server.addPlayer("TestPlayer");
        BukkitSource source = imperat.wrapSender(player);

        // 1) Verify Imperat's autoComplete API works directly
        var imperatResults = imperat.autoComplete(source, "greet ")
                                     .join();
        Assertions.assertLinesMatch(List.of("Mazen", "Ahmed", "Eyad"), imperatResults);

        // 2) Verify the Bukkit-side path: on Paper, tab completion goes through
        //    AsyncTabCompleteEvent (InternalBukkitCommand.tabComplete returns emptyList
        //    on Paper by design). We simulate the real Paper flow here.
        //    MockBukkit enforces that async events must be fired off the main thread.
        var event = new AsyncTabCompleteEvent(
                player,
                new ArrayList<>(),
                "/greet ",
                true,
                player.getLocation()
        );

        var thread = new Thread(() -> server.getPluginManager().callEvent(event));
        thread.start();
        thread.join();

        List<String> bukkitResults = event.getCompletions();
        Assertions.assertLinesMatch(List.of("Mazen", "Ahmed", "Eyad"), bukkitResults);


        //test for alias "salute"
        var event2 = new AsyncTabCompleteEvent(
                player,
                new ArrayList<>(),
                "/salute ",
                true,
                player.getLocation()
        );

        var thread2 = new Thread(() -> server.getPluginManager().callEvent(event2));
        thread2.start();
        thread2.join();

        List<String> bukkitResults2 = event2.getCompletions();
        Assertions.assertLinesMatch(List.of("Mazen", "Ahmed", "Eyad"), bukkitResults2);
    }
}



