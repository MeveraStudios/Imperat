package studio.mevera.imperat.annotations.base.system.parsers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import studio.mevera.imperat.Imperat
import studio.mevera.imperat.annotations.Default
import studio.mevera.imperat.annotations.base.AnnotationParser
import studio.mevera.imperat.annotations.base.MethodCommandExecutor
import studio.mevera.imperat.annotations.base.element.MethodElement
import studio.mevera.imperat.annotations.base.element.ParameterElement
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector
import studio.mevera.imperat.annotations.base.element.selector.MethodRules
import studio.mevera.imperat.annotations.base.element.selector.Rule
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

internal abstract class AbstractKotlinCommandClassParser<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : CommandElementParser<S>(imperat, parser, methodSelector) {

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

    /**
     * Override parseParameter to handle Kotlin-specific parameter features
     */
    override fun parseMethodParameter(method: MethodElement, param: ParameterElement): Argument<S>? {
        // Skip continuation parameters
        if (isContinuationParameter(param)) {
            return null
        }

        // Call parent to create base argument
        val argument = super.parseMethodParameter(method, param) ?: return null

        // Don't override Java @Default
        if (param.isAnnotationPresent(Default::class.java)) {
            return argument
        }

        val hasKotlinDefault = hasKotlinDefault(param)
        val isNullable = isKotlinNullable(param)

        if (!hasKotlinDefault && !isNullable) {
            return argument
        }

        ImperatDebugger.debug(
            "Parameter '${param.name}' has Kotlin default=$hasKotlinDefault, nullable=$isNullable"
        )

        // Create new argument with optional=true if it has Kotlin default or is nullable
        @Suppress("UNCHECKED_CAST")
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

    /**
     * Override parsePathway to handle suspend functions and Kotlin defaults
     */
    override fun parsePathwayMethod(
        command: Command<S>,
        method: MethodElement
    ): CommandPathway.Builder<S>? {
        // Call parent to create base pathway
        val basePathway = super.parsePathwayMethod(command, method) ?: return null

        val kFunction = method.element.kotlinFunction ?: return basePathway

        val hasDefaults = kFunction.parameters.any { it.kind == KParameter.Kind.VALUE && it.isOptional }
        val isSuspend = isSuspendFunction(method)

        return when {
            isSuspend -> handleSuspendFunction(method, basePathway)
            hasDefaults -> wrapWithKotlinDefaults(method, basePathway)
            else -> basePathway
        }
    }

    protected abstract fun handleSuspendFunction(
        method: MethodElement,
        basePathway: CommandPathway.Builder<S>
    ): CommandPathway.Builder<S>

    protected fun wrapWithKotlinDefaults(
        method: MethodElement,
        basePathway: CommandPathway.Builder<S>
    ): CommandPathway.Builder<S> {
        val originalExecutor = basePathway.execution as MethodCommandExecutor<S>
        val kFunction = method.element.kotlinFunction ?: return basePathway

        val wrappedExecutor = KotlinDefaultsAwareExecutor(
            originalExecutor,
            kFunction,
            false,
            null
        )

        return CommandPathway.builder<S>(method)
            .parameters(basePathway.parameters)
            .execute(wrappedExecutor)
            .permission(basePathway.permission)
            .description(basePathway.description)
            .examples(*basePathway.examples.toTypedArray())
            .apply {
                basePathway.cooldown?.let { cd ->
                    cooldown(cd.value(), cd.unit(), cd.permission())
                }
            }

    }

    protected fun wrapWithCoroutineSupport(
        method: MethodElement,
        basePathway: CommandPathway.Builder<S>
    ): CommandPathway.Builder<S> {
        val originalExecutor = basePathway.execution as MethodCommandExecutor<S>
        val kFunction = method.element.kotlinFunction ?: return basePathway

        val coroutineScope = imperat.config().coroutineScope as? CoroutineScope
            ?: throw IllegalStateException("CoroutineScope not available")

        val wrappedExecutor = KotlinDefaultsAwareExecutor(
            originalExecutor,
            kFunction,
            true,
            coroutineScope
        )

        return CommandPathway.builder<S>(method)
            .parameters(basePathway.parameters)
            .execute(wrappedExecutor)
            .permission(basePathway.permission)
            .description(basePathway.description)
            .examples(*basePathway.examples.toTypedArray())
            .apply {
                basePathway.cooldown?.let { cd ->
                    cooldown(cd.value(), cd.unit(), cd.permission())
                }
                coordinator(CoroutineCommandCoordinator(coroutineScope))
            }
    }

    /**
     * Executor that handles Kotlin defaults and coroutines
     */
    private class KotlinDefaultsAwareExecutor<S : Source>(
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

internal class KotlinCoroutineCommandClassParser<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandClassParser<S>(imperat, parser, methodSelector) {

    override fun handleSuspendFunction(
        method: MethodElement,
        basePathway: CommandPathway.Builder<S>
    ): CommandPathway.Builder<S> {
        return wrapWithCoroutineSupport(method, basePathway)
    }
}

/**
 * Basic visitor without coroutines
 */
internal class KotlinBasicCommandClassParser<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandClassParser<S>(imperat, parser, methodSelector) {

    override fun handleSuspendFunction(
        method: MethodElement,
        basePathway: CommandPathway.Builder<S>
    ): CommandPathway.Builder<S> {
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
    private val suspendFunctionRule = Rule.buildForMethod()
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
    ): CommandClassParser<S, Set<Command<S>>> {
        val selector = ElementSelector.create<MethodElement>()
            .addRule(MethodRules.HAS_KNOWN_SENDER)
            .addRule(suspendFunctionRule)

        return if (COROUTINES_AVAILABLE) {
            ImperatDebugger.debug("Kotlin coroutines ENABLED")
            KotlinCoroutineCommandClassParser(imperat, parser, selector)
        } else {
            ImperatDebugger.debug("Kotlin coroutines DISABLED")
            KotlinBasicCommandClassParser(imperat, parser, selector)
        }
    }
}