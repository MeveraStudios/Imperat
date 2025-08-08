package studio.mevera.imperat.exception.parse;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

import java.util.List;

public class WordOutOfRestrictionsException extends ParseException {

    private final List<String> restrictions;
    public WordOutOfRestrictionsException(String input, List<String> restrictions, Context<?> context) {
        super(input, context);
        this.restrictions = restrictions;
    }

    public List<String> getRestrictions() {
        return restrictions;
    }

}
