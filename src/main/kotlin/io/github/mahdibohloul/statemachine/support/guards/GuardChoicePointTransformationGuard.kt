package io.github.mahdibohloul.statemachine.support.guards

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.GuardDecision
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.guards.isAllowed
import io.github.mahdibohloul.statemachine.support.DefaultBehaviors
import reactor.core.publisher.Mono

class GuardChoicePointTransformationGuard<TContainer : TransformationContainer<*>>(
  val choicePoint: OnTransformationChoice<TContainer>,
  val baseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val chosenGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val otherwiseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
) : OnTransformationGuard<TContainer> {
  @Deprecated(
    "Legacy boolean-based guard execution method. Use executeDecision(container) instead.",
    replaceWith = ReplaceWith(
      "executeDecision(container: TContainer): Mono<GuardDecision>",
      "io.github.mahdibohloul.statemachine.guards.GuardDecision",
    ),
    level = DeprecationLevel.WARNING,
  )
  override fun execute(container: TContainer): Mono<Boolean> = baseGuard.execute(container)
    .filter { it }
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.execute(container)
      }
      return@flatMap otherwiseGuard.execute(container)
    }.defaultIfEmpty(false)

  override fun executeDecision(container: TContainer): Mono<GuardDecision> = baseGuard.executeDecision(container)
    .filter { it.isAllowed() }
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.executeDecision(container)
      }
      return@flatMap otherwiseGuard.executeDecision(container)
    }.defaultIfEmpty(GuardDecision.Deny())

  override fun validate(container: TContainer): Mono<Boolean> = baseGuard.validate(container)
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.validate(container)
      }
      return@flatMap otherwiseGuard.validate(container)
    }
}
