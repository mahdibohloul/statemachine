package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.StateMachineException
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import io.github.mahdibohloul.statemachine.support.transaction.StateTransformerTransactionSynchronization
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono

/**
 * Base interface for configuring and executing state machine transformation phases.
 *
 * This sealed interface defines the contract for different phases of state machine transformations.
 * Each implementation represents a specific phase in the transformation lifecycle and provides
 * the logic to execute that phase. The interface ensures type safety and enforces consistent
 * transformation patterns across different phases.
 *
 * ## Transformation Phases
 *
 * The state machine transformation process is divided into three main phases:
 *
 * 1. **Before Transformation** ([BeforeTransformationConfigurer]): Validation and pre-processing
 * 2. **During Transformation** ([DuringTransformationConfigurer]): Core transformation logic
 * 3. **After Transformation** ([AfterTransformationConfigurer]): Post-processing and cleanup
 *
 * ## Usage Pattern
 *
 * ```kotlin
 * // Configure different phases
 * val beforeConfig = BeforeTransformationConfigurer<MyContainer, MyEnum>()
 * val duringConfig = DuringTransformationConfigurer<MyContainer, MyEnum>()
 * val afterConfig = AfterTransformationConfigurer<MyContainer, MyEnum>()
 *
 * // Execute transformation phases
 * beforeConfig.transform(container)
 *   .flatMap { duringConfig.transform(it) }
 *   .flatMap { afterConfig.transform(it) }
 *   .subscribe { result ->
 *     // Handle transformation result
 *   }
 * ```
 *
 * ## Design Principles
 *
 * - **Reactive**: All operations return [Mono] for non-blocking execution
 * - **Composable**: Phases can be chained together using reactive operators
 * - **Type-safe**: Generic constraints ensure proper container and enum types
 * - **Extensible**: New transformation phases can be added by implementing this interface
 *
 * @param TContainer The type of transformation container that holds the state and data
 * @param TEnum The enum type representing possible states in the state machine
 */
sealed interface TransformationConfigurer<
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  > {
  /**
   * Executes the transformation phase for the given container.
   *
   * This method encapsulates the logic for a specific transformation phase.
   * The implementation should handle all necessary operations for that phase,
   * including validation, state changes, and any side effects.
   *
   * ## Implementation Guidelines
   *
   * - **Reactive**: Always return a [Mono] to maintain non-blocking behavior
   * - **Immutable**: Avoid modifying the input container directly; return a new or modified instance
   * - **Error Handling**: Use reactive error operators to handle exceptions gracefully
   * - **Validation**: Perform necessary validation before executing phase logic
   *
   * ## Common Patterns
   *
   * ### Simple Action Execution
   * ```kotlin
   * override fun transform(container: TContainer): Mono<TContainer> =
   *   action.execute(container)
   * ```
   *
   * ### Guarded Execution
   * ```kotlin
   * override fun transform(container: TContainer): Mono<TContainer> =
   *   guard.validate(container)
   *     .flatMap { action.execute(container) }
   * ```
   *
   * ### Chained Operations
   * ```kotlin
   * override fun transform(container: TContainer): Mono<TContainer> =
   *   action1.execute(container)
   *     .flatMap { action2.execute(it) }
   *     .flatMap { action3.execute(it) }
   * ```
   *
   * @param container The transformation container to process in this phase
   * @return A [Mono] containing the processed container after phase execution
   */
  fun transform(container: TContainer): Mono<TContainer>
}

/**
 * Configures and executes the pre-transformation phase of a state machine transformation.
 * This phase includes validation through guards and execution of pre-transformation actions.
 *
 * @param TContainer The type of transformation container that holds the state and data
 * @param TEnum The enum type representing possible states
 * @property beforeTransformationGuard The guard that validates the container before transformation
 * @property beforeTransformationAction The action to be executed before transformation
 */
class BeforeTransformationConfigurer<
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  >(
  var beforeTransformationGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  var beforeTransformationAction: OnTransformationAction<TContainer> = DefaultBehaviors.DefaultOnTransformationAction(),
) : TransformationConfigurer<TContainer, TEnum> {
  /**
   * Executes the pre-transformation phase by first validating the container
   * and then executing the pre-transformation action.
   *
   * @param container The transformation container to process
   * @return A Mono containing the processed container
   */
  override fun transform(container: TContainer): Mono<TContainer> = beforeTransformationGuard.validate(container)
    .flatMap { beforeTransformationAction.execute(container) }
}

/**
 * Configures and executes the during-transformation phase of a state machine transformation.
 * This phase focuses on executing the main transformation action.
 *
 * @param TContainer The type of transformation container that holds the state and data
 * @param TEnum The enum type representing possible states
 * @property duringTransformationAction The action to be executed during transformation
 */
class DuringTransformationConfigurer<
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  >(
  var duringTransformationAction: OnTransformationAction<TContainer> = DefaultBehaviors.DefaultOnTransformationAction(),
) : TransformationConfigurer<TContainer, TEnum> {
  /**
   * Executes the during-transformation phase by running the transformation action.
   *
   * @param container The transformation container to process
   * @return A Mono containing the processed container
   */
  override fun transform(container: TContainer): Mono<TContainer> = duringTransformationAction.execute(container)
}

/**
 * Configures and executes the post-transformation phase of a state machine transformation.
 * This phase includes validation, execution of post-transformation actions,
 * and registration of transaction synchronization.
 *
 * @param TContainer The type of transformation container that holds the state and data
 * @param TEnum The enum type representing possible states
 * @property afterTransformationGuard The guard that validates the container after transformation
 * @property afterTransformationAction The action to be executed after transformation
 * @property afterCommitTransactionAction The action to be executed after the transaction commit.
 * This action will only be executed if the state machine transformation is performed within an active transaction
 * (e.g., MongoDB transaction) and the transaction is successfully committed.
 * If the transaction rolls back or if no transaction is active, this action will be skipped.
 * This is useful for operations that should only happen after the transaction is guaranteed to be committed,
 * such as sending notifications or triggering external system updates.
 */
class AfterTransformationConfigurer<
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  >(
  var afterTransformationGuard: OnTransformationGuard<TContainer> =
    DefaultBehaviors.DefaultOnTransformationGuard(),
  var afterTransformationAction: OnTransformationAction<TContainer> =
    DefaultBehaviors.DefaultOnTransformationAction(),
  var afterCommitTransactionAction: OnTransformationAction<TContainer> =
    DefaultBehaviors.DefaultOnTransformationAction(),
) : TransformationConfigurer<TContainer, TEnum> {
  /**
   * Executes the post-transformation phase by:
   * 1. Running the post-transformation action (which may include transaction operations)
   * 2. Validating the container after transformation
   * 3. Registering transaction synchronization for post-commit actions
   *
   * The transaction synchronization ensures that [afterCommitTransactionAction] is only executed
   * after the transaction is successfully committed, providing a guarantee that the main
   * transformation operations have been persisted.
   *
   * @param container The transformation container to process
   * @return A Mono containing the processed container
   */
  override fun transform(
    container: TContainer,
  ): Mono<TContainer> = afterTransformationAction.execute(container)
    .flatMap { afterTransformationGuard.validate(it).thenReturn(it) }
    .flatMap(::registerTransactionSynchronizer)

  /**
   * Registers a transaction synchronizer for post-commit actions.
   * This synchronizer will execute the [afterCommitTransactionAction] only after
   * the transaction is successfully committed.
   * If no transaction is active, the container is returned as is.
   *
   * @param container The transformation container to process
   * @return A Mono containing the processed container
   */
  private fun registerTransactionSynchronizer(
    container: TContainer,
  ): Mono<TContainer> = TransactionSynchronizationManager
    .forCurrentTransaction()
    .map { tsm ->
      if (tsm.isActualTransactionActive) {
        tsm.registerSynchronization(
          StateTransformerTransactionSynchronization(
            afterCommitAction = afterCommitTransactionAction,
            container = container,
          ),
        )
      }
      return@map container
    }.onErrorReturn(NoTransactionException::class.java, container)
}

/**
 * Main configurer for the state machine transformation process.
 * This class coordinates the entire transformation process, including error handling,
 * container management, and state transitions.
 *
 * @param TRequest The type of transformation request
 * @param TContainer The type of transformation container
 * @param TResponse The type of transformation response
 * @param TEnum The enum type representing possible states
 * @property onTransformationErrorHandler Handler for transformation errors
 * @property transformationContainerProvider Provider for transformation containers
 * @property transformationResponseProvider Provider for transformation responses
 * @property sourceState The source state for the transformation
 * @property targetState The target state for the transformation
 */
class StateMachineConfigurer<
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<TEnum>,
  TResponse : Any,
  TEnum : Enum<*>,
  >(
  var onTransformationErrorHandler: OnTransformationErrorHandler<TRequest, TResponse> =
    DefaultBehaviors.DefaultOnTransformationErrorHandler(),
  var transformationContainerProvider: TransformationContainerProvider<TRequest, TContainer, TEnum> =
    DefaultBehaviors.DefaultTransformationContainerProvider(),
  var transformationResponseProvider: TransformationResponseProvider<TRequest, TContainer, TResponse> =
    DefaultBehaviors.DefaultTransformationResponseProvider(),
  var sourceState: TEnum? = null,
  var targetState: TEnum? = null,
) {
  /**
   * Validates the state machine configuration.
   * Throws an exception if source and target states are the same.
   *
   * @throws StateMachineException.SourceAndTargetAreEqualException if source and target states are identical
   */
  fun validate() {
    if (sourceState == targetState) {
      throw StateMachineException.SourceAndTargetAreEqualException(
        source = sourceState,
        target = targetState,
      )
    }
  }
}
