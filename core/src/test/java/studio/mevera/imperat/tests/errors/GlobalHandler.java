package studio.mevera.imperat.tests.errors;

import studio.mevera.imperat.annotations.ExceptionHandler;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.tests.TestSource;

public final class GlobalHandler {
    
    @ExceptionHandler(CustomException.class)
    public void t(CustomException ex, Context<TestSource> ctx) {
        System.out.println("IT WORKS !");
    }
    
}
