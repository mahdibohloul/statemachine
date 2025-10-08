package io.github.mahdibohloul.statemachine.providers

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import reactor.core.publisher.Mono

/**
 * The transformation response provider is used to provide a response for the caller
 * at the end of the transformation process.
 *
 * If there is no response extractor, the response will be an empty complete signal to the caller and
 * If there are multiple response extractors, the first with no empty complete will be used.
 *
 * @param TRequest The type of the transformation request
 * @param TContainer The type of the transformation container
 * @param TResponse The type of the transformation response
 * @see TransformationContainer
 * @see TransformationRequest
 * @see io.github.mahdibohloul.statemachine.support.DefaultBehaviors.DefaultTransformationResponseProvider
 */
interface TransformationResponseProvider<
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<*>,
  TResponse : Any,
  > {
  fun provideResponse(request: TRequest, container: TContainer): Mono<TResponse>
}
