package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.exception.AmbiguousCommandException;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousFourOptionalBooleans;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousSameTypeInt;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousSameTypeOptional;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousSameTypeRequired;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousThreeOptional;
import studio.mevera.imperat.tests.commands.ambiguity.AmbiguousThreeRequiredStrings;
import studio.mevera.imperat.tests.commands.ambiguity.InvalidGreedyInMiddle;
import studio.mevera.imperat.tests.commands.ambiguity.InvalidGreedyWithOptional;
import studio.mevera.imperat.tests.commands.ambiguity.ValidCommand;
import studio.mevera.imperat.tests.commands.ambiguity.ValidDifferentTypes;
import studio.mevera.imperat.tests.commands.ambiguity.ValidGreedyAtEnd;
import studio.mevera.imperat.tests.commands.ambiguity.ValidMixedNature;
import studio.mevera.imperat.tests.commands.ambiguity.ValidMultipleOptionalDifferentTypes;


@DisplayName("Ambiguity Detection Test")
public class AmbiguityDetectingTest extends EnhancedBaseImperatTest {

    // ==================== AMBIGUOUS COMMANDS ====================

    private final static TestImperat IMPERAT = ImperatTestGlobals.IMPERAT;

    @Test
    @DisplayName("Should detect ambiguity: Two required parameters with same type (String)")
    void testAmbiguousSameTypeRequired() {
        try {

            IMPERAT.registerCommand(AmbiguousSameTypeRequired.class);
            Assertions.fail();
        }catch(AmbiguousCommandException ex) {
        }
    }

    @Test
    @DisplayName("Should detect ambiguity: Two optional parameters with same type (String)")
    void testAmbiguousSameTypeOptional() {
        try {
            IMPERAT.registerCommand(AmbiguousSameTypeOptional.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    @Test
    @DisplayName("Should detect ambiguity: Multiple required integers (int)")
    void testAmbiguousSameTypeInt() {
        try {
            IMPERAT.registerCommand(AmbiguousSameTypeInt.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    @Test
    @DisplayName("Should detect ambiguity: Three optional parameters with same type (String)")
    void testAmbiguousThreeOptional() {
        try {
            IMPERAT.registerCommand(AmbiguousThreeOptional.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    @Test
    @DisplayName("Should detect ambiguity: Three required strings")
    void testAmbiguousThreeRequiredStrings() {
        try {
            IMPERAT.registerCommand(AmbiguousThreeRequiredStrings.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    @Test
    @DisplayName("Should detect ambiguity: Four optional booleans")
    void testAmbiguousFourOptionalBooleans() {
        try {
            IMPERAT.registerCommand(AmbiguousFourOptionalBooleans.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    // ==================== INVALID GREEDY COMMANDS ====================

    @Test
    @DisplayName("Should throw error: Greedy parameter in the middle")
    void testInvalidGreedyInMiddle() {
        try {
            IMPERAT.registerCommand(InvalidGreedyInMiddle.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    @Test
    @DisplayName("Should throw error: Greedy parameter with optional parameter after it")
    void testInvalidGreedyWithOptional() {
        try {
            IMPERAT.registerCommand(InvalidGreedyWithOptional.class);
            Assertions.fail("Expected AmbiguousCommandException to be thrown");
        } catch (AmbiguousCommandException ex) {
            // Expected exception
        }
    }

    // ==================== VALID COMMANDS ====================

    @Test
    @DisplayName("Should allow: Valid command with different types")
    void testValidDifferentTypes() {
        try {
            IMPERAT.registerCommand(ValidDifferentTypes.class);
        } catch (AmbiguousCommandException ex) {
            Assertions.fail("Should allow command with different parameter types");
        }
    }

    @Test
    @DisplayName("Should allow: Valid command with mixed nature (required and optional)")
    void testValidMixedNature() {
        try {
            IMPERAT.registerCommand(ValidMixedNature.class);
        } catch (AmbiguousCommandException ex) {
            Assertions.fail("Should allow command with mixed required and optional parameters of same type");
        }
    }

    @Test
    @DisplayName("Should allow: Greedy parameter at the end")
    void testValidGreedyAtEnd() {
        try {
            IMPERAT.registerCommand(ValidGreedyAtEnd.class);
        } catch (AmbiguousCommandException ex) {
            Assertions.fail("Should allow command with greedy parameter at the end");
        }
    }

    @Test
    @DisplayName("Should allow: Basic valid command")
    void testValidCommand() {
        try {
            IMPERAT.registerCommand(ValidCommand.class);
        } catch (AmbiguousCommandException ex) {
            Assertions.fail("Should allow basic valid command");
        }
    }

    @Test
    @DisplayName("Should allow: Multiple optional parameters with different types")
    void testValidMultipleOptionalDifferentTypes() {
        try {
            IMPERAT.registerCommand(ValidMultipleOptionalDifferentTypes.class);
        } catch (AmbiguousCommandException ex) {
            Assertions.fail("Should allow command with multiple optional parameters of different types");
        }
    }
}
