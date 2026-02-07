package studio.mevera.imperat.command

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import studio.mevera.imperat.Imperat
import studio.mevera.imperat.context.ExecutionContext
import studio.mevera.imperat.context.Source
import studio.mevera.imperat.util.ImperatDebugger

/**
 * Command coordinator that executes commands in a coroutine scope.
 * Provides proper structured concurrency and automatic cleanup.
 */
class CoroutineCommandCoordinator<S : Source>(
    val scope: CoroutineScope
) : CommandCoordinator<S> {
    override fun coordinate(imperat: Imperat<S>, source: S, context: ExecutionContext<S>, execution: CommandExecution<S>) {
        scope.launch {
            try {
                execution.execute(source, context)
            } catch (e: Exception) {
                ImperatDebugger.error(
                    CoroutineCommandCoordinator::class.java,
                    "execute",
                    e
                )
                throw e
            }
        }
    }
}