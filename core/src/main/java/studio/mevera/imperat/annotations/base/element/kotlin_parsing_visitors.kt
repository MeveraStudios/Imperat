package studio.mevera.imperat.annotations.base.element

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import studio.mevera.imperat.Imperat
import studio.mevera.imperat.annotations.Default
import studio.mevera.imperat.annotations.base.AnnotationParser
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector
import studio.mevera.imperat.annotations.base.element.selector.MethodRules
import studio.mevera.imperat.command.Command
import studio.mevera.imperat.command.CommandUsage
import studio.mevera.imperat.command.CoroutineCommandCoordinator
import studio.mevera.imperat.command.parameters.Argument
import studio.mevera.imperat.command.parameters.OptionalValueSupplier
import studio.mevera.imperat.context.ExecutionContext
import studio.mevera.imperat.context.Source
import studio.mevera.imperat.util.ImperatDebugger
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Abstract base class for Kotlin-aware command parsing visitors.
 * Provides common functionality for handling Kotlin default parameters.
 */
internal abstract class AbstractKotlinCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : CommandParsingVisitor<S>(imperat, parser, methodSelector) {

    /**
     * Checks if a method is a Kotlin suspend function
     */
    protected fun isSuspendFunction(method: MethodElement): Boolean {
        val parameters = method.element.parameters
        return parameters.isNotEmpty() &&
                parameters.last().type == Continuation::class.java
    }

    /**
     * Extracts Kotlin default parameter values using reflection.
     */
    protected fun extractKotlinDefaultValue(
        parameter: ParameterElement
    ): OptionalValueSupplier? {
        try {
            val methodElement = parameter.parent as? MethodElement ?: return null
            val kFunction = methodElement.element.kotlinFunction ?: return null

            // Find the corresponding KParameter
            val kParameter = kFunction.parameters.find { kParam ->
                kParam.kind == KParameter.Kind.VALUE &&
                        kParam.name == parameter.name &&
                        kParam.type.javaType == parameter.element.parameterizedType
            } ?: return null

            // Check if the parameter has a default value in Kotlin
            if (!kParameter.isOptional) {
                return null
            }

            // Create a supplier that uses Kotlin's default value mechanism
            return object : OptionalValueSupplier {
                override fun <S : Source> supply(
                    context: ExecutionContext<S>,
                    argument: Argument<S>
                ): String? {
                    ImperatDebugger.debug(
                        "Parameter '${parameter.name}' has Kotlin default value"
                    )
                    return null
                }

                override fun isEmpty(): Boolean = false
            }

        } catch (e: Exception) {
            ImperatDebugger.error(javaClass, "extractKotlinDefaultValue", e)
        }
        return null
    }

    /**
     * Override loadParameter to handle Kotlin default values
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> loadParameter(parameter: ParameterElement): Argument<S>? {
        val kotlinDefault = extractKotlinDefaultValue(parameter)
        val argument = super.loadParameter<T>(parameter)

        if (kotlinDefault != null &&
            argument != null &&
            !parameter.isAnnotationPresent(Default::class.java)
        ) {
            return Argument.of(
                argument.name(),
                argument.type(),
                argument.permissionsData,
                argument.description,
                true, // Mark as optional since Kotlin has default
                argument.isGreedy,
                kotlinDefault, // Use Kotlin default supplier
                argument.suggestionResolver,
                argument.validators.toList()
            ) as Argument<S>
        }

        return argument
    }

    /**
     * Hook for handling suspend functions - implemented by subclasses
     */
    protected abstract fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandUsage<S>
    ): CommandUsage<S>?

    /**
     * Override loadUsage to delegate suspend function handling to subclasses
     */
    override fun loadUsage(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement
    ): CommandUsage<S>? {
        val usage = super.loadUsage(parentCmd, loadedCmd, method) ?: return null

        if (isSuspendFunction(method)) {
            return handleSuspendFunction(parentCmd, loadedCmd, method, usage)
        }

        return usage
    }
}

/**
 * Kotlin command parsing visitor with full coroutine support.
 * Used when kotlinx-coroutines is available on the classpath.
 */
internal class KotlinCoroutineCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandParsingVisitor<S>(imperat, parser, methodSelector) {
    val coroutineScope = imperat.config().coroutineScope as? CoroutineScope ?: throw IllegalArgumentException("Coroutine scope is not set")

    override fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandUsage<S>
    ): CommandUsage<S> {
        val wrappedExecutor = createSuspendExecutor(usage, method)

        val cooldown = usage.cooldown
        var builder = CommandUsage.builder<S>()
            .parameters(usage.parameters)
            .execute(wrappedExecutor)
            .permission(usage.permissionsData)
            .description(usage.description)
            .examples(*usage.examples.toTypedArray())
            .coordinator(CoroutineCommandCoordinator(coroutineScope))

        cooldown?.let {
            builder = builder.cooldown(it.value(), it.unit(), it.permission())
        }

        return builder.build(loadedCmd, usage.isHelp)
    }

    private fun createSuspendExecutor(
        usage: CommandUsage<S>,
        method: MethodElement
    ): studio.mevera.imperat.annotations.base.MethodCommandExecutor<S> {
        val originalExecutor = usage.execution as studio.mevera.imperat.annotations.base.MethodCommandExecutor<S>
        val kFunction = method.element.kotlinFunction ?: return originalExecutor

        return object : studio.mevera.imperat.annotations.base.MethodCommandExecutor<S>(originalExecutor) {
            override fun execute(source: S, context: ExecutionContext<S>) {
                coroutineScope.launch {
                    val args = originalExecutor.prepareArguments(context)
                    val parameterMap = buildParameterMap(kFunction, args, originalExecutor.boundMethodCaller.instance())
                    kFunction.callSuspendBy(parameterMap)
                }
            }

            private fun buildParameterMap(
                kFunction: KFunction<*>,
                args: Array<Any?>,
                instance: Any
            ): Map<KParameter, Any?> {
                val parameterMap = mutableMapOf<KParameter, Any?>()

                kFunction.instanceParameter?.let {
                    parameterMap[it] = instance
                }

                val valueParameters = kFunction.parameters
                    .filter { it.kind == KParameter.Kind.VALUE }

                args.forEachIndexed { index, arg ->
                    if (index < valueParameters.size && arg != null) {
                        parameterMap[valueParameters[index]] = arg
                    }
                }

                return parameterMap
            }
        }
    }
}

internal class KotlinBasicCommandParsingVisitor<S : Source>(
    imperat: Imperat<S>,
    parser: AnnotationParser<S>,
    methodSelector: ElementSelector<MethodElement>
) : AbstractKotlinCommandParsingVisitor<S>(imperat, parser, methodSelector) {

    override fun handleSuspendFunction(
        @Nullable parentCmd: Command<S>?,
        @NotNull loadedCmd: Command<S>,
        method: MethodElement,
        usage: CommandUsage<S>
    ): CommandUsage<S>? {
        throw IllegalStateException(
            "Method '${method.name}' is a suspend function but kotlinx.coroutines is not available on the classpath. " +
                    "Add 'org.jetbrains.kotlinx:kotlinx-coroutines-core' dependency or convert the method to a regular function."
        )
    }
}

/**
 * Factory for creating the appropriate Kotlin command parsing visitor
 * based on classpath availability of kotlinx-coroutines.
 */
/**
 * Factory for creating the appropriate Kotlin command parsing visitor
 * based on classpath availability of kotlinx-coroutines.
 */
@Suppress("unused") // used by reflection
internal object KotlinCommandParsingVisitorFactory {

    /**
     * Checks if Kotlin coroutines runtime is available on the classpath
     */
    @JvmStatic
    private val COROUTINES_AVAILABLE: Boolean by lazy {
        try {
            Class.forName("kotlinx.coroutines.CoroutineScope")
            Class.forName("kotlinx.coroutines.BuildersKt")
            Class.forName("kotlinx.coroutines.SupervisorJob")
            true
        } catch (e: ClassNotFoundException) {
            ImperatDebugger.debug("Kotlin coroutines not found on classpath - suspend functions will not be supported")
            false
        }
    }

    /**
     * Static check for suspend functions - used in validation rules
     */
    @JvmStatic
    private fun isSuspendFunctionStatic(method: MethodElement): Boolean {
        val parameters = method.element.parameters
        return parameters.isNotEmpty() &&
                parameters.last().type == Continuation::class.java
    }

    /**
     * Creates validation rule for suspend functions based on coroutines availability
     */
    @JvmStatic
    private val suspendFunctionRule = studio.mevera.imperat.annotations.base.element.selector.Rule.buildForMethod()
        .condition { _: Imperat<*>, _: AnnotationParser<*>, method: MethodElement ->
            // Allow method if it's not a suspend function, or if it is and coroutines are available
            !isSuspendFunctionStatic(method) || COROUTINES_AVAILABLE
        }
        .failure { _: AnnotationParser<*>, method: MethodElement ->
            val msg = "Method '${method.name}' is a suspend function but kotlinx.coroutines is not available on the classpath. " +
                    "Add 'org.jetbrains.kotlinx:kotlinx-coroutines-core' dependency or convert the method to a regular function."
            throw MethodRules.methodError(method, msg)
        }
        .build()

    /**
     * Factory method to create the appropriate Kotlin-aware visitor
     * based on classpath availability.
     */
    @JvmStatic
    fun <S : Source> create(
        imperat: Imperat<S>,
        parser: AnnotationParser<S>
    ): CommandClassVisitor<S, Set<Command<S>>> {
        val selector = ElementSelector.create<MethodElement>()
            .addRule(MethodRules.HAS_KNOWN_SENDER)
            .addRule(suspendFunctionRule)

        return if (COROUTINES_AVAILABLE) {
            ImperatDebugger.debug("Creating KotlinCoroutineCommandParsingVisitor - coroutines support enabled")
            KotlinCoroutineCommandParsingVisitor(imperat, parser, selector)
        } else {
            ImperatDebugger.debug("Creating KotlinBasicCommandParsingVisitor - coroutines support disabled")
            KotlinBasicCommandParsingVisitor(imperat, parser, selector)
        }
    }
}