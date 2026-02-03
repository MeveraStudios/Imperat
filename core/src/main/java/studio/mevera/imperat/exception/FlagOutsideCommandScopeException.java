package studio.mevera.imperat.exception;

import studio.mevera.imperat.command.Command;

public class FlagOutsideCommandScopeException extends CommandException{

    private final Command<?> wrongCmd;
    private final String flagInput;
    public FlagOutsideCommandScopeException(Command<?> wrongCmd, String flagInput) {
        super();
        this.wrongCmd = wrongCmd;
        this.flagInput = flagInput;
    }

    public Command<?> getWrongCmd() {
        return wrongCmd;
    }

    public String getFlagInput() {
        return flagInput;
    }
}
