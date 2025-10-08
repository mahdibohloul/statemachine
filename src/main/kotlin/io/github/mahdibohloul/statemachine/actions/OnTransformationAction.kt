package io.github.mahdibohloul.statemachine.actions

import io.github.mahdibohloul.statemachine.TransformationContainer
import reactor.core.publisher.Mono

/**
 * Actions are one of the most useful components that you can use to interact and collaborate with a state machine.
 * You can run actions in various places in a state machine and its states' lifecycle.
 * @param TContainer The container that holds the transformation data.
 * @see TransformationContainer
 * @see io.github.mahdibohloul.statemachine.support.DefaultBehaviors.DefaultOnTransformationAction
 */
interface OnTransformationAction<TContainer : TransformationContainer<*>> {
  fun execute(container: TContainer): Mono<TContainer>
}
