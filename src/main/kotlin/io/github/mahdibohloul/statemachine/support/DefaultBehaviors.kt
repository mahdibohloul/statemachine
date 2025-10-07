package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.StateMachineException
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

internal class DefaultBehaviors {
  class DefaultOnTransformationAction<TContainer : TransformationContainer<*>> : OnTransformationAction<TContainer> {
    override fun execute(container: TContainer): Mono<TContainer> = container.toMono()
  }

  class DefaultOnTransformationErrorHandler<
    TRequest : TransformationRequest,
    TResponse : Any,
    > : OnTransformationErrorHandler<TRequest, TResponse> {
    override fun onError(request: TRequest, error: Throwable): Mono<TResponse> = Mono.error(error)
  }

  class DefaultOnTransformationGuard<TContainer : TransformationContainer<*>> : OnTransformationGuard<TContainer> {
    override fun execute(container: TContainer): Mono<Boolean> = true.toMono()
  }

  class DefaultTransformationResponseProvider<
    TRequest : TransformationRequest,
    TContainer : TransformationContainer<*>,
    TResponse : Any,
    > : TransformationResponseProvider<TRequest, TContainer, TResponse> {
    override fun provideResponse(request: TRequest, container: TContainer): Mono<TResponse> = Mono.empty()
  }

  class DefaultTransformationContainerProvider<
    TRequest : TransformationRequest,
    TContainer : TransformationContainer<TEnum>,
    TEnum : Enum<*>,
    > : TransformationContainerProvider<TRequest, TContainer, TEnum> {
    override fun provideContainer(
      request: TRequest,
      source: TEnum?,
      target: TEnum?,
    ): Mono<TContainer> = Mono.error(StateMachineException.NoContainerProviderConfiguredException(source, target))
  }
}
