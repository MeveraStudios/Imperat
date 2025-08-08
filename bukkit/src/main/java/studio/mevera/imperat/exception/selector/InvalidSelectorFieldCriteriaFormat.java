package studio.mevera.imperat.exception.selector;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ParseException;

public class InvalidSelectorFieldCriteriaFormat extends ParseException {

    private final String fieldCriteriaInput;

    public InvalidSelectorFieldCriteriaFormat(String fieldCriteriaInput, String fullInput, Context<?> ctx) {
        super(fullInput, ctx);
        this.fieldCriteriaInput = fieldCriteriaInput;
    }

    public String getFieldCriteriaInput() {
        return fieldCriteriaInput;
    }
}
