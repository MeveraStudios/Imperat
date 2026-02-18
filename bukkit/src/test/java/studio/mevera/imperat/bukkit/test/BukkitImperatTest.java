package studio.mevera.imperat.bukkit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.ExecutionResult;

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
}



