package io.github.mahdibohloul.statemachine.support.actions

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.support.DefaultBehaviors
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

class ActionChoicePointTransformationAction<TContainer : TransformationContainer<*>>(
  val choicePoint: OnTransformationChoice<TContainer>,
  val baseAction: OnTransformationAction<TContainer> = DefaultBehaviors.DefaultOnTransformationAction(),
  val chosenAction: OnTransformationAction<TContainer> = DefaultBehaviors.DefaultOnTransformationAction(),
  val otherwiseAction: OnTransformationAction<TContainer> = DefaultBehaviors.DefaultOnTransformationAction(),
) : OnTransformationAction<TContainer> {
  override fun execute(container: TContainer): Mono<TContainer> = baseAction.execute(container)
    .zipWhen { transformedContainer -> choicePoint.isChosen(transformedContainer) }
    .flatMap { (transformedContainer, isChosen) ->
      if (isChosen) {
        return@flatMap chosenAction.execute(transformedContainer)
      }
      return@flatMap otherwiseAction.execute(transformedContainer)
    }
}
