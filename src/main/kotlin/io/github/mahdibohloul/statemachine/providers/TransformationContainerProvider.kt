package io.github.mahdibohloul.statemachine.providers

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import reactor.core.publisher.Mono

/**
 * The transformation container provider is used to provide a container for the transformation
 * and the provided container will be used in the transformation process
 * and will be propagated in the chain of the actions and guards.
 *
 * If there is no container provider behavior, an error will be thrown.
 *
 * If there are multiple container provider behaviors, the first with no empty complete will be used.
 *
 * @param TRequest The type of the transformation request
 * @param TContainer The type of the transformation container
 * @param TEnum The type of the enum that matches the transformation container
 * @see TransformationContainer
 * @see TransformationRequest
 * @see io.github.mahdibohloul.statemachine.support.DefaultBehaviors.DefaultTransformationResponseProvider
 */
interface TransformationContainerProvider<
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<TEnum>,
  TEnum : Enum<*>,
  > {
  fun provideContainer(request: TRequest, source: TEnum?, target: TEnum?): Mono<TContainer>
}
