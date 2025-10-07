package io.github.mahdibohloul.statemachine.guards

import box.tapsi.libs.utilities.ErrorCodeString
import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.StateMachineErrorCodeString
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
   * Executes the provided transformation guard logic against the specified container.
   *
   * @param container The container representing the transformation context on which the guard logic will be executed.
   * @return A Mono that emits a Boolean indicating whether the guard condition was satisfied (true) or not (false).
   */
  fun execute(container: TContainer): Mono<Boolean>

  /**
   * Validates the given transformation container using a guard and emits the result.
   * If validation fails, an error is emitted encapsulating details about the failure.
   *
   * @param container The transformation container to be validated.
   * @return A Mono emitting `true` if validation succeeds, otherwise emits an error.
   */
  fun validate(container: TContainer): Mono<Boolean> = execute(container)
    .handle { validationRes, sink: SynchronousSink<Boolean> ->
      if (validationRes) {
        return@handle sink.next(validationRes)
      }
      return@handle sink.error(
        StateMachineException.GuardValidationException(
          guardName = this::class.getOriginalClass().simpleName.orEmpty(),
          validationFailureErrorCodeString = this.getValidationFailureErrorCodeString(),
          source = container.source,
          target = container.target,
          cause = getValidationFailureCause(),
        ),
      )
    }

  /**
   * Provides the error code string associated with a guard validation failure.
   *
   * @return The specific error code string representing guard validation failure.
   */
  fun getValidationFailureErrorCodeString(): ErrorCodeString = StateMachineErrorCodeString.GuardValidationFailed
  /**
   * Retrieves the cause of the validation failure, if available.
   *
   * This function is used to provide additional context or details
   * about why a validation operation has failed. It returns a throwable
   * that represents the root cause of the failure, or null if no specific
   * cause is provided.
   *
   * @return A Throwable representing the cause of the validation failure, or null if no cause is available.
   */
  fun getValidationFailureCause(): Throwable? = null
}
