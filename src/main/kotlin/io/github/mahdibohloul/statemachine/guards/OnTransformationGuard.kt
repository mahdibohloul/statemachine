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
  @Deprecated(
    message = "Legacy boolean-based guard execution method. Use executeDecision(container) instead.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
      "executeDecision(container: TContainer): Mono<GuardDecision>",
      "io.github.mahdibohloul.statemachine.guards.GuardDecision",
    ),
  )
  fun execute(container: TContainer): Mono<Boolean>

  /**
   * Executes the guard and maps the result to a GuardDecision.
   *
   * This is the preferred, new-style API for guard implementations.
   * The default implementation adapts legacy boolean-based guards by using
   * [getValidationFailureErrorCodeString] and [getValidationFailureCause] to produce a [GuardDecision].
   *
   * New implementations should override this method directly instead of [execute].
   *
   * @param container The transformation container to be validated.
   * @return A Mono emitting a GuardDecision based on the execution result.
   */
  fun executeDecision(container: TContainer): Mono<GuardDecision> = execute(container)
    .map { allowed ->
      if (allowed) {
        GuardDecision.Allow
      } else {
        GuardDecision.Deny(
          errorCode = getValidationFailureErrorCodeString(),
          cause = getValidationFailureCause(),
        )
      }
    }

  /**
   * Validates the given transformation container using a guard and emits the result.
   * If validation fails, an error is emitted encapsulating details about the failure.
   *
   * This method is built on top of [executeDecision] and works seamlessly with both legacy boolean-based guards
   * and new-style guards returning [GuardDecision].
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

  /**
   * Provides the error code string associated with a guard validation failure.
   *
   * This method is only consulted when [executeDecision] is not overridden.
   * It serves as a legacy extension point for boolean-based guards and is kept for backward compatibility.
   *
   * @return The specific error code string representing guard validation failure.
   */
  @Deprecated(
    message = "Legacy hook for boolean-based guards. Override executeDecision(container) " +
      "and return GuardDecision.Deny(errorCode, cause) instead.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
      "executeDecision(container: TContainer): Mono<GuardDecision>",
      "io.github.mahdibohloul.statemachine.guards.GuardDecision",
    ),
  )
  fun getValidationFailureErrorCodeString(): ErrorCodeString = StateMachineErrorCodeString.GuardValidationFailed

  /**
   * Retrieves the cause of the validation failure, if available.
   *
   * This method is only used by the default [executeDecision] adapter for legacy boolean-based guards.
   * New guards should encode the cause directly in [GuardDecision.Deny] returned from [executeDecision].
   *
   * @return A Throwable representing the cause of the validation failure, or null if no cause is available.
   */
  @Deprecated(
    message = "Legacy hook for boolean-based guards. Provide failure details via GuardDecision.Deny " +
      "in executeDecision(container) instead.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
      "executeDecision(container: TContainer): Mono<GuardDecision>",
      "io.github.mahdibohloul.statemachine.guards.GuardDecision",
    ),
  )
  fun getValidationFailureCause(): Throwable? = null
}
