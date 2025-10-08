@file:Suppress("detekt.TooManyFunctions")

package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import io.github.mahdibohloul.statemachine.support.actions.ActionChoicePointTransformationAction
import io.github.mahdibohloul.statemachine.support.guards.GuardChoicePointTransformationGuard

/**
 * Chains two transformation actions together, creating a composite action that executes them sequentially.
 * If the receiver is already a composite action, the new action is added to the existing chain.
 * Otherwise, a new composite action is created containing both actions.
 *
 * ```kotlin
 * // Chain multiple actions together
 * beforeTransformationAction = onTransformationActionFactory.getAction(
 *   DeliveryRequestSaveTransformationAction::class,
 *   DeliveryRequestOrderCreationTransformationAction::class,
 *   DeliveryRequestChangeLifeStageTransformationAction::class,
 * )
 *
 * // Or chain them explicitly
 * val action1 = onTransformationActionFactory.getAction(DeliveryRequestSaveTransformationAction::class)
 * val action2 = onTransformationActionFactory.getAction(DeliveryRequestOrderCreationTransformationAction::class)
 * val chainedAction = action1 andThen action2
 * ```
 *
 * @param anotherAction The action to be executed after the current action
 * @return A composite action that executes both actions in sequence
 */
infix fun <TContainer : TransformationContainer<*>> OnTransformationAction<TContainer>.andThen(
  anotherAction: OnTransformationAction<TContainer>,
): OnTransformationAction<TContainer> = if (this is CompositeBehaviors.CompositeOnTransformationAction) {
  apply { this.addAction(anotherAction) }
} else {
  CompositeBehaviors.CompositeOnTransformationAction(mutableListOf(this, anotherAction))
}

/**
 * Chains two error handlers together, creating a composite error handler that attempts to handle errors
 * using the first handler, and if that fails, falls back to the second handler.
 * If the receiver is already a composite error handler, the new handler is added to the existing chain.
 *
 * ```kotlin
 * onTransformationErrorHandler = onTransformationErrorHandlerFactory.getErrorHandler(
 *   DeliveryRequestModificationDuplicationTransformationErrorHandler::class,
 *   DeliveryRequestModificationInsufficientBalanceTransformationErrorHandler::class,
 * )
 * ```
 *
 * @param anotherErrorHandler The error handler to be used as a fallback
 * @return A composite error handler that chains both handlers
 */
infix fun <TRequest : TransformationRequest, TResponse : Any> OnTransformationErrorHandler<TRequest, TResponse>.andThen(
  anotherErrorHandler: OnTransformationErrorHandler<TRequest, TResponse>,
): OnTransformationErrorHandler<TRequest, TResponse> = if (
  this is CompositeBehaviors.CompositeOnTransformationErrorHandler
) {
  apply { this.addErrorHandler(anotherErrorHandler) }
} else {
  CompositeBehaviors.CompositeOnTransformationErrorHandler(mutableListOf(this, anotherErrorHandler))
}

/**
 * Chains two transformation guards together, creating a composite guard that executes them sequentially.
 * All guards must return true for the composite guard to return true.
 * If the receiver is already a composite guard, the new guard is added to the existing chain.
 *
 * ```kotlin
 * // Chain multiple guards together
 * beforeTransformationGuard = onTransformationGuardFactory.getGuard(
 *   DeliveryRequestSourceConsistenceTargetTransformationGuard::class,
 *   DeliveryRequestPayerTransformationGuard::class,
 *   DeliveryRequestCarOnDemandFulfillmentPackageTransformationGuard::class,
 * )
 *
 * // Or chain them explicitly
 * val guard1 = onTransformationGuardFactory.getGuard(DeliveryRequestAuthorizationTransformationGuard::class)
 * val guard2 = onTransformationGuardFactory.getGuard(DeliveryRequestSourceConsistenceTargetTransformationGuard::class)
 * val chainedGuard = guard1 andThen guard2
 * ```
 *
 * @param anotherGuard The guard to be executed after the current guard
 * @return A composite guard that executes both guards in sequence
 */
infix fun <TContainer : TransformationContainer<*>> OnTransformationGuard<TContainer>.andThen(
  anotherGuard: OnTransformationGuard<TContainer>,
): OnTransformationGuard<TContainer> = if (this is CompositeBehaviors.CompositeOnTransformationGuard) {
  apply { this.addGuard(anotherGuard) }
} else {
  CompositeBehaviors.CompositeOnTransformationGuard(mutableListOf(this, anotherGuard))
}

/**
 * Chains two response providers together, creating a composite provider that attempts to provide a response
 * using the first provider, and if that returns empty, falls back to the second provider.
 * If the receiver is already a composite provider, the new provider is added to the existing chain.
 *
 * ```kotlin
 * // Multiple providers can be chained to provide fallback responses
 * transformationResponseProvider = primaryResponseProvider andThen fallbackResponseProvider
 * ```
 *
 * @param anotherProvider The response provider to be used as a fallback
 * @return A composite response provider that chains both providers
 */
infix fun <
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<*>,
  TResponse : Any,
  > TransformationResponseProvider<TRequest, TContainer, TResponse>.andThen(
  anotherProvider: TransformationResponseProvider<TRequest, TContainer, TResponse>,
): TransformationResponseProvider<TRequest, TContainer, TResponse> = if (
  this is CompositeBehaviors.CompositeTransformationResponseProvider
) {
  apply { this.addProvider(anotherProvider) }
} else {
  CompositeBehaviors.CompositeTransformationResponseProvider(mutableListOf(this, anotherProvider))
}

/**
 * Chains two container providers together, creating a composite provider that attempts to provide a container
 * using the first provider, and if that returns empty, falls back to the second provider.
 * If the receiver is already a composite provider, the new provider is added to the existing chain.
 *
 * ```kotlin
 * // Multiple container providers can be chained for fallback scenarios
 * transformationContainerProvider = primaryContainerProvider andThen fallbackContainerProvider
 * ```
 *
 * @param anotherProvider The container provider to be used as a fallback
 * @return A composite container provider that chains both providers
 */
infix fun <
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  > TransformationContainerProvider<TRequest, TContainer, TEnum>.andThen(
  anotherProvider: TransformationContainerProvider<TRequest, TContainer, TEnum>,
): TransformationContainerProvider<TRequest, TContainer, TEnum> = if (
  this is CompositeBehaviors.CompositeTransformationContainerProvider
) {
  apply { this.addProvider(anotherProvider) }
} else {
  CompositeBehaviors.CompositeTransformationContainerProvider(mutableListOf(this, anotherProvider))
}

/**
 * Creates a choice point for a transformation action, allowing conditional execution based on a choice.
 * This creates an [ActionChoicePointTransformationAction] that can be further configured
 * with chosen and otherwise actions.
 *
 * ```kotlin
 * // Create a choice point for system user vs regular user actions
 * beforeTransformationAction = beforeTransformationAction withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class)
 *
 * // Complete choice point configuration
 * val choicePoint = onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class)
 * val action = onTransformationActionFactory.getAction(
 *    DeliveryRequestReplaceTripInformationTransformationAction::class
 * )
 * val choiceAction = action withChoice choicePoint
 * ```
 *
 * @param choicePoint The choice that determines which action path to take
 * @return An action choice point that can be configured with chosen and otherwise actions
 */
infix fun <TContainer : TransformationContainer<*>> OnTransformationAction<TContainer>.withChoice(
  choicePoint: OnTransformationChoice<TContainer>,
): ActionChoicePointTransformationAction<TContainer> = ActionChoicePointTransformationAction(
  baseAction = this,
  choicePoint = choicePoint,
)

/**
 * Configures the chosen action for an action choice point.
 * This action will be executed when the choice point evaluates to true.
 *
 * ```kotlin
 * beforeTransformationAction = beforeTransformationAction withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class) withChosen
 *   onTransformationActionFactory.getAction(DeliveryRequestReplaceTripInformationTransformationAction::class)
 * ```
 *
 * @param chosenAction The action to execute when the choice is selected
 * @return The configured action choice point
 */
infix fun <TContainer : TransformationContainer<*>> ActionChoicePointTransformationAction<TContainer>.withChosen(
  chosenAction: OnTransformationAction<TContainer>,
): ActionChoicePointTransformationAction<TContainer> = ActionChoicePointTransformationAction(
  baseAction = this.baseAction,
  choicePoint = this.choicePoint,
  chosenAction = chosenAction,
  otherwiseAction = this.otherwiseAction,
)

/**
 * Configures the otherwise action for an action choice point.
 * This action will be executed when the choice point evaluates to false.
 *
 * ```kotlin
 * beforeTransformationAction = configurer.beforeTransformationAction withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class) otherwise
 *   onTransformationActionFactory.getAction(
 *     DeliveryRequestExternalCarOnDemandOrderInvoiceModificationTransformationAction::class,
 *   ) andThen
 *   onTransformationActionFactory.getAction(DeliveryRequestReplaceInvoiceTransformationAction::class)
 * ```
 *
 * @param otherwiseAction The action to execute when the choice is not selected
 * @return The configured action choice point
 */
infix fun <TContainer : TransformationContainer<*>> ActionChoicePointTransformationAction<TContainer>.otherwise(
  otherwiseAction: OnTransformationAction<TContainer>,
): ActionChoicePointTransformationAction<TContainer> = ActionChoicePointTransformationAction(
  baseAction = this.baseAction,
  choicePoint = this.choicePoint,
  chosenAction = this.chosenAction,
  otherwiseAction = otherwiseAction,
)

/**
 * Creates a choice point for a transformation guard, allowing conditional guard execution based on a choice.
 * This creates a [GuardChoicePointTransformationGuard] that can be further configured with chosen and otherwise guards.
 *
 * ```kotlin
 * // Create a choice point for system user vs regular user guards
 * beforeTransformationGuard = configurer.beforeTransformationGuard andThen
 *   onTransformationGuardFactory.getGuard(
 *     DeliveryRequestModificationDuplicationTransformationGuard::class,
 *     DeliveryRequestCarOnDemandFulfillmentPackageTransformationGuard::class,
 *   ) withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class)
 * ```
 *
 * @param choicePoint The choice that determines which guard path to take
 * @return A guard choice point that can be configured with chosen and otherwise guards
 */
infix fun <TContainer : TransformationContainer<*>> OnTransformationGuard<TContainer>.withChoice(
  choicePoint: OnTransformationChoice<TContainer>,
): GuardChoicePointTransformationGuard<TContainer> = GuardChoicePointTransformationGuard(
  baseGuard = this,
  choicePoint = choicePoint,
)

/**
 * Configures the chosen guard for a guard choice point.
 * This guard will be executed when the choice point evaluates to true.
 *
 * ```kotlin
 * // Configure chosen guard for system users
 * val guardChoice = baseGuard withChoice systemUserChoice
 * val configuredGuard = guardChoice withChosen systemUserGuard
 * ```
 *
 * @param chosenGuard The guard to execute when the choice is selected
 * @return The configured guard choice point
 */
infix fun <TContainer : TransformationContainer<*>> GuardChoicePointTransformationGuard<TContainer>.withChosen(
  chosenGuard: OnTransformationGuard<TContainer>,
): GuardChoicePointTransformationGuard<TContainer> = GuardChoicePointTransformationGuard(
  baseGuard = this.baseGuard,
  choicePoint = this.choicePoint,
  chosenGuard = chosenGuard,
  otherwiseGuard = this.otherwiseGuard,
)

/**
 * Configures the otherwise guard for a guard choice point.
 * This guard will be executed when the choice point evaluates to false.
 *
 * ```kotlin
 * // Complete guard choice point configuration
 * beforeTransformationGuard = configurer.beforeTransformationGuard andThen
 *   onTransformationGuardFactory.getGuard(
 *     DeliveryRequestModificationDuplicationTransformationGuard::class,
 *     DeliveryRequestCarOnDemandFulfillmentPackageTransformationGuard::class,
 *   ) withChoice
 *   onTransformationChoiceFactory.getChoice(DeliverySystemUserTransformationChoice::class) otherwise
 *   onTransformationGuardFactory.getGuard(
 *     DeliveryRequestModifiableStageTransformationGuard::class,
 *     DeliveryRequestExternalOrderPaymentMethodEligibilityTransformationGuard::class,
 *   )
 * ```
 *
 * @param otherwiseGuard The guard to execute when the choice is not selected
 * @return The configured guard choice point
 */
infix fun <TContainer : TransformationContainer<*>> GuardChoicePointTransformationGuard<TContainer>.otherwise(
  otherwiseGuard: OnTransformationGuard<TContainer>,
): GuardChoicePointTransformationGuard<TContainer> = GuardChoicePointTransformationGuard(
  baseGuard = this.baseGuard,
  choicePoint = this.choicePoint,
  chosenGuard = this.chosenGuard,
  otherwiseGuard = otherwiseGuard,
)
