package studio.mevera.imperat.annotations.base.element

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import studio.mevera.imperat.Imperat
import studio.mevera.imperat.annotations.Default
import studio.mevera.imperat.annotations.base.AnnotationParser
import studio.mevera.imperat.annotations.base.MethodCommandExecutor
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector
import studio.mevera.imperat.annotations.base.element.selector.MethodRules
import studio.mevera.imperat.command.Command
import studio.mevera.imperat.command.CommandPathway
import studio.mevera.imperat.command.CoroutineCommandCoordinator
import studio.mevera.imperat.command.parameters.Argument
import studio.mevera.imperat.context.ExecutionContext
import studio.mevera.imperat.context.Source
import studio.mevera.imperat.util.ImperatDebugger
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction

internal abstract class AbstractKotlinCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : CommandParsingVisitor<S>(imperat, parser, methodSelector) {
    protected fun isSuspendFunction(method: MethodElement): Boolean =
        method.element.parameters.isNotEmpty() &&
                method.element.parameters.last().type == Continuation::class.java

    protected fun isContinuationParameter(parameter: ParameterElement): Boolean {
        val type = parameter.element.type
        return type == Continuation::class.java ||
                type.name == "kotlin.coroutines.Continuation" ||
                type.interfaces.any { it.name == "kotlin.coroutines.Continuation" } ||
                Continuation::class.java.isAssignableFrom(type)
    }

    protected fun isKotlinNullable(parameter: ParameterElement): Boolean {
        try {
            val kParam = findKValueParameterByIndex(parameter)
                ?: ((parameter.parent as? MethodElement)?.element?.kotlinFunction?.let { findKParameter(it, parameter) })
                ?: return false
            return kParam.type.isMarkedNullable
        } catch (_: Exception) {
            return false
        }
    }

    protected fun findKParameter(kFunction: KFunction<*>, parameter: ParameterElement): KParameter? =
        kFunction.parameters.find { kParam ->
            kParam.kind == KParameter.Kind.VALUE &&
                    kParam.name == parameter.name &&
                    kParam.type.javaType == parameter.element.parameterizedType
        }

    private fun findKValueParameterByIndex(parameter: ParameterElement): KParameter? {
        val method = parameter.parent as? MethodElement ?: return null
        val kFunction = method.element.kotlinFunction ?: return null

        val kValueParams = kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }
        if (kValueParams.isEmpty()) {
            return null
        }

        var javaValueIndex = 0
        for (methodParam in method.parameters) {
            if (methodParam === parameter) {
                break
            }
            if (isContinuationParameter(methodParam) || methodParam.isContextResolved) {
                continue
            }
            javaValueIndex++
        }

        return kValueParams.getOrNull(javaValueIndex)
    }

    protected fun hasKotlinDefault(parameter: ParameterElement): Boolean {
        try {
            val kParam = findKValueParameterByIndex(parameter)
                ?: ((parameter.parent as? MethodElement)?.element?.kotlinFunction?.let { findKParameter(it, parameter) })
                ?: return false
            return kParam.isOptional
        } catch (_: Exception) {
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> loadParameter(parameter: ParameterElement): Argument<S>? {
        if (isContinuationParameter(parameter)) {
            return null
        }

        val argument = super.loadParameter<T>(parameter) ?: return null

        // Don't override Java @Default
        if (parameter.isAnnotationPresent(Default::class.java)) {
            return argument
        }

        val hasKotlinDefault = hasKotlinDefault(parameter)

        val isNullable = isKotlinNullable(parameter)

        if (!hasKotlinDefault && !isNullable) {
            return argument
        }

        ImperatDebugger.debug(
            "Parameter '${parameter.name}' has Kotlin default=$hasKotlinDefault, nullable=$isNullable"
        )

        return Argument.of(
            argument.getName(),
            argument.type(),
            argument.permissionsData,
            argument.description,
            true,  // Mark as optional
            argument.isGreedy,
            argument.defaultValueSupplier,
            argument.suggestionResolver,
            argument.validators.toList()
        ) as Argument<S>
    }

    protected abstract fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandPathway<S>
    ): CommandPathway<S>?

    override fun loadPathway(parentCmd: Command<S>?, loadedCmd: Command<S>, method: MethodElement): CommandPathway<S>? {
        val usage = super.loadPathway(parentCmd, loadedCmd, method) ?: return null
        val kFunction = method.element.kotlinFunction ?: return usage

        val hasDefaults = kFunction.parameters.any { it.kind == KParameter.Kind.VALUE && it.isOptional }

        return when {
            isSuspendFunction(method) -> handleSuspendFunction(parentCmd, loadedCmd, method, usage)
            hasDefaults -> wrapWithKotlinHandling(loadedCmd, method, usage, false)
            else -> usage  // Plain Kotlin function, no wrapping needed
        }
    }

    protected fun wrapWithKotlinHandling(
        loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandPathway<S>,
        isSuspend: Boolean
    ): CommandPathway<S> {
        val originalExecutor = usage.execution as MethodCommandExecutor<S>
        val kFunction = method.element.kotlinFunction ?: return usage

        val wrappedExecutor = KotlinAwareExecutor(
            originalExecutor,
            kFunction,
            isSuspend,
            if (isSuspend) (imperat.config().coroutineScope as? CoroutineScope) else null
        )

        return CommandPathway.builder<S>(method)
            .parameters(usage.arguments)
            .execute(wrappedExecutor)
            .permission(usage.permissionsData)
            .description(usage.description)
            .examples(*usage.examples.toTypedArray())
            .apply {
                usage.cooldown?.let { cd ->
                    cooldown(cd.value(), cd.unit(), cd.permission())
                }
                if (isSuspend) {
                    coordinator(CoroutineCommandCoordinator(imperat.config().coroutineScope as CoroutineScope))
                }
            }
            .build(loadedCmd)
    }

    /**
     * Executor that handles Kotlin defaults by omitting null parameters that have defaults
     */
    private class KotlinAwareExecutor<S : Source>(
        originalExecutor: MethodCommandExecutor<S>,
        private val kFunction: KFunction<*>,
        private val isSuspend: Boolean,
        private val coroutineScope: CoroutineScope?
    ) : MethodCommandExecutor<S>(originalExecutor) {

        override fun execute(source: S, context: ExecutionContext<S>) {
            val args = super.prepareArguments(context)
            val paramMap = buildParamMap(args)
            if (isSuspend) {
                val scope = coroutineScope
                    ?: throw IllegalStateException("Suspend function found but no coroutine scope")
                scope.launch {
                    val returned = kFunction.callSuspendBy(paramMap)
                    handleReturnValue(context, returned)
                }
            } else {
                val returned = kFunction.callBy(paramMap)
                handleReturnValue(context, returned)
            }
        }

        private fun handleReturnValue(context: ExecutionContext<S>, returned: Any?) {
            val method = methodElement
            if (method.returnType == Void.TYPE) {
                return
            }
            @Suppress("UNCHECKED_CAST")
            val returnResolver = context.imperatConfig()
                    .getReturnResolver<Any>(method.returnType)
                ?: return

            returnResolver.handle(context, method, returned)
        }

        private fun buildParamMap(args: Array<Any?>): Map<KParameter, Any?> {
            val map = mutableMapOf<KParameter, Any?>()

            kFunction.extensionReceiverParameter?.let {
                throw IllegalStateException("Extension receiver parameters are not supported yet.")
            }

            kFunction.instanceParameter?.let { map[it] = boundMethodCaller.instance() }

            val valueParams = kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }

            args.forEachIndexed { index, arg ->
                if (index < valueParams.size) {
                    val kParam = valueParams[index]
                    if (arg == null && kParam.isOptional) {
                        ImperatDebugger.debug("Omitting '${kParam.name}' — using Kotlin default")
                    } else {
                        map[kParam] = arg
                    }
                }
            }

            return map
        }
    }
}

internal class KotlinCoroutineCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandParsingVisitor<S>(imperat, parser, methodSelector) {

    override fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandPathway<S>
    ): CommandPathway<S> {
        return wrapWithKotlinHandling(loadedCmd, method, usage, true)
    }
}

/**
 * Basic visitor without coroutines
 */
internal class KotlinBasicCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandParsingVisitor<S>(imperat, parser, methodSelector) {

    override fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandPathway<S>
    ): CommandPathway<S> {
        throw IllegalStateException(
            "Suspend function '${method.name}' requires kotlinx-coroutines-core dependency"
        )
    }
}

@Suppress("unused")
internal object KotlinCommandParsingVisitorFactory {

    @JvmStatic
    private val COROUTINES_AVAILABLE: Boolean by lazy {
        try {
            Class.forName("kotlinx.coroutines.CoroutineScope")
            Class.forName("kotlinx.coroutines.BuildersKt")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    @JvmStatic
    private fun isSuspendFunctionStatic(method: MethodElement): Boolean =
        method.element.parameters.isNotEmpty() &&
                method.element.parameters.last().type == Continuation::class.java

    @JvmStatic
    private val suspendFunctionRule = studio.mevera.imperat.annotations.base.element.selector.Rule.buildForMethod()
        .condition { _: Imperat<*>, _: AnnotationParser<*>, method: MethodElement ->
            !isSuspendFunctionStatic(method) || COROUTINES_AVAILABLE
        }
        .failure { _: AnnotationParser<*>, method: MethodElement ->
            throw MethodRules.methodError(method, "Suspend function requires kotlinx-coroutines-core")
        }
        .build()

    @JvmStatic
    fun <S : Source> create(
        imperat: Imperat<S>,
        parser: AnnotationParser<S>
    ): CommandClassVisitor<S, Set<Command<S>>> {
        val selector = ElementSelector.create<MethodElement>()
            .addRule(MethodRules.HAS_KNOWN_SENDER)
            .addRule(suspendFunctionRule)

        return if (COROUTINES_AVAILABLE) {
            ImperatDebugger.debug("Kotlin coroutines ENABLED")
            KotlinCoroutineCommandParsingVisitor(imperat, parser, selector)
        } else {
            ImperatDebugger.debug("Kotlin coroutines DISABLED")
            KotlinBasicCommandParsingVisitor(imperat, parser, selector)
        }
    }
}