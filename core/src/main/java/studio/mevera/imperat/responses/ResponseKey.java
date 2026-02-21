package studio.mevera.imperat.responses;

import studio.mevera.imperat.util.Keyed;

public interface ResponseKey extends Keyed<String> {

    // Parse exceptions
    ResponseKey INVALID_BOOLEAN = () -> "args.parsing.invalid-boolean";
    ResponseKey INVALID_ENUM = () -> "args.parsing.invalid-enum";
    ResponseKey INVALID_NUMBER_FORMAT = () -> "args.parsing.invalid-number-format";
    ResponseKey INVALID_MAP_ENTRY_FORMAT = () -> "args.parsing.invalid-map-entry-format";
    ResponseKey INVALID_UUID = () -> "args.parsing.invalid-uuid";
    ResponseKey WORD_OUT_OF_RESTRICTIONS = () -> "args.parsing.word-out-of-restrictions";
    ResponseKey VALUE_OUT_OF_CONSTRAINT = () -> "args.parsing.value-out-of-constraint";

    // Flag-related exceptions
    ResponseKey UNKNOWN_FLAG = () -> "flag.unknown";
    ResponseKey MISSING_FLAG_INPUT = () -> "flag.missing-input";
    ResponseKey FLAG_OUTSIDE_SCOPE = () -> "flag.outside-scope";

    // Validation exceptions
    ResponseKey NUMBER_OUT_OF_RANGE = () -> "args.validation.number-out-of-range";

    // Permission exceptions
    ResponseKey PERMISSION_DENIED = () -> "permission.denied";

    // Command exceptions
    ResponseKey INVALID_SYNTAX = () -> "command.invalid-syntax";
    ResponseKey COOLDOWN = () -> "command.cooldown";

    // Help exceptions
    ResponseKey NO_HELP = () -> "help.not-available";
    ResponseKey NO_HELP_PAGE = () -> "help.page-not-found";

}
