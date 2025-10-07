package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
import io.github.mahdibohloul.statemachine.transformers.StateTransformer

fun StateMachineSupportResult.isSuccess(): Boolean = this is StateMachineSupportResult.Success

fun StateMachineSupportResult.asFailure(): StateMachineSupportResult.Failure = this as StateMachineSupportResult.Failure

@Suppress("detekt.TooGenericExceptionCaught")
internal fun StateTransformer<*, *, *>.checkAsResult(
  transformerIdentifier: StateMachineStateFactory.TransformerIdentifier,
): StateMachineSupportResult = try {
  this.canHandle(transformerIdentifier)
  StateMachineSupportResult.success()
} catch (e: Exception) {
  StateMachineSupportResult.failure(e)
}
