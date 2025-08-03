package studio.mevera.imperat.annotations.base;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.command.CommandExecution;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.returns.ReturnResolver;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.util.asm.DefaultMethodCallerFactory;
import studio.mevera.imperat.util.asm.MethodCaller;

import java.util.List;

@ApiStatus.Internal
public class MethodCommandExecutor<S extends Source> implements CommandExecution<S> {

    private final Imperat<S> dispatcher;
    private final MethodElement method;
    private final MethodCaller.BoundMethodCaller boundMethodCaller;
    private final List<CommandParameter<S>> fullParameters;

    private MethodCommandExecutor(
        Imperat<S> dispatcher,
        MethodElement method,
        List<CommandParameter<S>> fullParameters
    ) {

        try {
            this.dispatcher = dispatcher;
            this.method = method;

            ClassElement methodOwner = (ClassElement) method.getParent();
            boundMethodCaller = DefaultMethodCallerFactory.INSTANCE.createFor(method.getElement()).bindTo(methodOwner.getObjectInstance());

            this.fullParameters = fullParameters;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        //this.helpAnnotation = help;
    }

    public static <S extends Source> MethodCommandExecutor<S> of(
        Imperat<S> imperat,
        MethodElement method,
        List<CommandParameter<S>> fullParameters
    ) {
        return new MethodCommandExecutor<>(imperat, method, fullParameters);
    }


    /**
     * Executes the command's actions
     *
     * @param source  the source/sender of this command
     * @param context the context of the command
     */
    @Override
    public void execute(S source, ExecutionContext<S> context) throws ImperatException {

        var instances = AnnotationHelper.loadParameterInstances(
            dispatcher, fullParameters,
            source, context, method
        );

        Object returned = boundMethodCaller.call(instances);
        if (method.getReturnType() == void.class) {
            return;
        }

        ReturnResolver<S, Object> returnResolver = context.imperatConfig().getReturnResolver(method.getReturnType());
        if (returnResolver == null) {
            return;
        }

        returnResolver.handle(context, method, returned);

    }

}
