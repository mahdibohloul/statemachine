package io.github.mahdibohloul.statemachine.guards

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.StateMachineException
import io.github.mahdibohloul.statemachine.TransformationContainer
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink

/**
 * Interface defining a guard mechanism for transformations. A guard is responsible
 * for validating the transformation based on specific conditions or checks. If the
 * validation fails, an appropriate exception is thrown.
 *
 * @param TContainer The type of the transformation container that extends TransformationContainer.
 */
interface OnTransformationGuard<TContainer : TransformationContainer<*>> {
  /**
   * Executes the guard and maps the result to a [GuardDecision].
   *
   * @param container The transformation container to be validated.
   * @return A Mono emitting a GuardDecision based on the execution result.
   */
  fun executeDecision(container: TContainer): Mono<GuardDecision>

  /**
   * Validates the given transformation container using a guard and emits the result.
   * If validation fails, an error is emitted encapsulating details about the failure.
   *
   * This method is built on top of [executeDecision].
   *
   * @param container The transformation container to be validated.
   * @return A Mono emitting `true` if validation succeeds, otherwise emits an error.
   */
  fun validate(container: TContainer): Mono<Boolean> = executeDecision(container)
    .handle { decision, sink: SynchronousSink<Boolean> ->
      when (decision) {
        GuardDecision.Allow -> sink.next(true)
        is GuardDecision.Deny -> sink.error(
          StateMachineException.GuardValidationException(
            guardName = this::class.getOriginalClass().simpleName.orEmpty(),
            validationFailureErrorCodeString = decision.errorCode,
            source = container.source,
            target = container.target,
            cause = decision.cause,
          ),
        )
      }
    }
}
