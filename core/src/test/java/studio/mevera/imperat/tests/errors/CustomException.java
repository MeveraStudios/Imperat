package studio.mevera.imperat.tests.errors;

import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.TestSource;

public class CustomException extends ImperatException {

    public CustomException(Context<TestSource> ctx) {
        super(ctx);
        
    }
}
