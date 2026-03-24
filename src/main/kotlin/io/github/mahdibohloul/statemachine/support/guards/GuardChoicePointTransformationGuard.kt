package io.github.mahdibohloul.statemachine.support.guards

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.GuardDecision
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.guards.isDenied
import io.github.mahdibohloul.statemachine.support.DefaultBehaviors
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class GuardChoicePointTransformationGuard<TContainer : TransformationContainer<*>>(
  val choicePoint: OnTransformationChoice<TContainer>,
  val baseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val chosenGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val otherwiseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
) : OnTransformationGuard<TContainer> {
  override fun executeDecision(container: TContainer): Mono<GuardDecision> = baseGuard.executeDecision(container)
    .flatMap { baseDecision ->
      if (baseDecision.isDenied()) {
        return@flatMap baseDecision.toMono()
      }

      return@flatMap choicePoint.isChosen(container)
        .flatMap { isChosen ->
          if (isChosen) {
            chosenGuard.executeDecision(container)
          } else {
            otherwiseGuard.executeDecision(container)
          }
        }
    }

  override fun validate(container: TContainer): Mono<Boolean> = baseGuard.validate(container)
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.validate(container)
      }
      return@flatMap otherwiseGuard.validate(container)
    }
}
