package io.github.mahdibohloul.statemachine.choices

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.factories.OnTransformationChoiceFactory
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import io.github.mahdibohloul.statemachine.support.actions.ActionChoicePointTransformationAction
import io.github.mahdibohloul.statemachine.support.guards.GuardChoicePointTransformationGuard
import reactor.core.publisher.Mono

/**
 * Represents a choice point in a state machine transformation workflow.
 *
 * This interface implements the choice/junction pattern from state machine theory, allowing
 * conditional branching based on the current state of a transformation container. Choice points
 * enable dynamic decision-making during state machine execution, where different transformation
 * paths can be taken based on business logic conditions.
 *
 * Choice points are used in
 * conjunction with [ActionChoicePointTransformationAction] and [GuardChoicePointTransformationGuard]
 * to create conditional execution flows.
 * They can be combined using the [CompositeBehaviors.CompositeOnTransformationChoice] to create complex decision trees.
 *
 * ## Usage Examples
 *
 * ### Basic Choice Implementation
 * ```kotlin
 * @Component
 * class DeliverySystemUserTransformationChoice : OnTransformationChoice<DeliveryRequestTransformationContainer<*>> {
 *   override fun isChosen(container: DeliveryRequestTransformationContainer<*>): Mono<Boolean> = Mono.fromCallable {
 *     container.user == DefaultUser.systemUser
 *   }
 * }
 * ```
 *
 * ### Choice Point in Action Configuration
 * ```kotlin
 * // Configure conditional action execution
 * beforeTransformationAction = beforeTransformationAction withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class) withChosen
 *   onTransformationActionFactory.getAction(DeliveryRequestReplaceTripInformationTransformationAction::class)
 * ```
 *
 * ### Choice Point in Guard Configuration
 * ```kotlin
 * // Configure conditional guard execution
 * beforeTransformationGuard = baseGuard withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class) otherwise
 *   onTransformationGuardFactory.getGuard(DeliveryRequestModifiableStageTransformationGuard::class)
 * ```
 *
 * ## Design Principles
 *
 * - **Single Responsibility**: Each choice should evaluate a single, well-defined condition
 * - **Reactive**: All operations should be non-blocking and return [Mono<Boolean>]
 * - **Stateless**: Choices should not maintain state between invocations
 * - **Composable**: Choices can be combined to create complex decision logic
 *
 * ## Integration with State Machine Components
 *
 * - **Actions**: Used with [ActionChoicePointTransformationAction] to conditionally execute transformation actions
 * - **Guards**: Used with [GuardChoicePointTransformationGuard] to conditionally apply validation rules
 * - **Factories**: Discovered and managed by [OnTransformationChoiceFactory]
 * - **Composition**: Combined using [CompositeBehaviors.CompositeOnTransformationChoice]
 *
 * @param TContainer The type of transformation container that this choice operates on
 * @see ActionChoicePointTransformationAction
 * @see GuardChoicePointTransformationGuard
 * @see OnTransformationChoiceFactory
 * @see CompositeBehaviors.CompositeOnTransformationChoice
 * @see [branches-choice-junction-points](https://www.ibm.com/docs/en/dmrt/9.5.0?topic=branches-choice-junction-points)
 */
interface OnTransformationChoice<TContainer : TransformationContainer<*>> {
  /**
   * Determines whether this choice point should be selected based on the current container state.
   *
   * This method evaluates the business logic condition for this choice point. The result
   * determines which branch of the transformation workflow should be executed.
   *
   * ## Implementation Guidelines
   *
   * - **Reactive**: Always return a [Mono<Boolean>] to maintain non-blocking behavior
   * - **Deterministic**: The same container state should always produce the same result
   * - **Fast**: Evaluation should be quick and not involve expensive operations
   * - **Safe**: Handle exceptions gracefully and return appropriate fallback values
   *
   * ## Common Patterns
   *
   * ### User-based Choices
   * ```kotlin
   * override fun isChosen(container: TContainer): Mono<Boolean> = Mono.fromCallable {
   *   container.user == DefaultUser.systemUser
   * }
   * ```
   *
   * ### Status-based Choices
   * ```kotlin
   * override fun isChosen(container: TContainer): Mono<Boolean> = Mono.fromCallable {
   *   container.status == DeliveryRequestStatus.Creation
   * }
   * ```
   *
   * ### Complex Business Logic
   * ```kotlin
   * override fun isChosen(container: TContainer): Mono<Boolean> = Mono.fromCallable {
   *   container.hasValidPaymentMethod() && container.isWithinTimeWindow()
   * }
   * ```
   *
   * @param container The transformation container containing the current state and data
   * @return A [Mono] emitting `true` if this choice should be selected, `false` otherwise
   */
  fun isChosen(container: TContainer): Mono<Boolean>
}
