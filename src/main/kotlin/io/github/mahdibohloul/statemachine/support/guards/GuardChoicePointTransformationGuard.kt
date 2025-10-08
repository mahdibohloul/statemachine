package io.github.mahdibohloul.statemachine.support.guards

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.support.DefaultBehaviors
import reactor.core.publisher.Mono

class GuardChoicePointTransformationGuard<TContainer : TransformationContainer<*>>(
  val choicePoint: OnTransformationChoice<TContainer>,
  val baseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val chosenGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
  val otherwiseGuard: OnTransformationGuard<TContainer> = DefaultBehaviors.DefaultOnTransformationGuard(),
) : OnTransformationGuard<TContainer> {
  override fun execute(container: TContainer): Mono<Boolean> = baseGuard.execute(container)
    .filter { it }
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.execute(container)
      }
      return@flatMap otherwiseGuard.execute(container)
    }.defaultIfEmpty(false)

  override fun validate(container: TContainer): Mono<Boolean> = baseGuard.validate(container)
    .flatMap { choicePoint.isChosen(container) }
    .flatMap { isChosen ->
      if (isChosen) {
        return@flatMap chosenGuard.validate(container)
      }
      return@flatMap otherwiseGuard.validate(container)
    }
}
