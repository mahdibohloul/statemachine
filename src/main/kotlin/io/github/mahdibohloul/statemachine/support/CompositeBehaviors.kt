package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

class CompositeBehaviors {
  class CompositeOnTransformationAction<TContainer : TransformationContainer<*>>(
    private val actions: MutableList<OnTransformationAction<TContainer>>,
  ) : OnTransformationAction<TContainer> {
    override fun execute(container: TContainer): Mono<TContainer> = actions.fold(container.toMono()) { acc, action ->
      acc.flatMap { action.execute(it) }
    }

    fun addAction(action: OnTransformationAction<TContainer>) {
      actions.add(action)
    }

    fun getActionCount(): Int = actions.size
  }

  class CompositeOnTransformationErrorHandler<TRequest : TransformationRequest, TResponse : Any>(
    private val errorHandlers: MutableList<OnTransformationErrorHandler<TRequest, TResponse>>,
  ) : OnTransformationErrorHandler<TRequest, TResponse> {
    override fun onError(
      request: TRequest,
      error: Throwable,
    ): Mono<TResponse> = errorHandlers.fold(Mono.error(error)) { acc, errorHandler ->
      acc.onErrorResume { errorHandler.onError(request, it) }
    }

    fun addErrorHandler(errorHandler: OnTransformationErrorHandler<TRequest, TResponse>) {
      errorHandlers.add(errorHandler)
    }

    fun getErrorHandlerCount(): Int = errorHandlers.size
  }

  class CompositeOnTransformationGuard<TContainer : TransformationContainer<*>>(
    private val guards: MutableList<OnTransformationGuard<TContainer>>,
  ) : OnTransformationGuard<TContainer> {
    override fun execute(container: TContainer): Mono<Boolean> = guards.fold(true.toMono()) { acc, guard ->
      acc.filter { it }.flatMap { guard.execute(container) }.defaultIfEmpty(false)
    }

    override fun validate(container: TContainer): Mono<Boolean> = guards.fold(true.toMono()) { acc, guard ->
      acc.flatMap { guard.validate(container) }
    }

    fun addGuard(guard: OnTransformationGuard<TContainer>) {
      guards.add(guard)
    }

    fun getGuardCount(): Int = guards.size
  }

  class CompositeTransformationResponseProvider<
    TRequest : TransformationRequest,
    TContainer : TransformationContainer<*>,
    TResponse : Any,
    >(
    private val providers: MutableList<TransformationResponseProvider<TRequest, TContainer, TResponse>>,
  ) : TransformationResponseProvider<TRequest, TContainer, TResponse> {
    override fun provideResponse(
      request: TRequest,
      container: TContainer,
    ): Mono<TResponse> = providers.fold(Mono.empty()) { acc, provider ->
      acc.switchIfEmpty { provider.provideResponse(request, container) }
    }

    fun addProvider(provider: TransformationResponseProvider<TRequest, TContainer, TResponse>) {
      providers.add(provider)
    }

    fun getProviderCount(): Int = providers.size
  }

  class CompositeTransformationContainerProvider<
    TRequest : TransformationRequest,
    TContainer : TransformationContainer<TEnum>,
    TEnum : Enum<*>,
    >(
    private val providers: MutableList<TransformationContainerProvider<TRequest, TContainer, TEnum>>,
  ) : TransformationContainerProvider<TRequest, TContainer, TEnum> {
    override fun provideContainer(
      request: TRequest,
      source: TEnum?,
      target: TEnum?,
    ): Mono<TContainer> = providers.fold(Mono.empty()) { acc, provider ->
      acc.switchIfEmpty { provider.provideContainer(request, source, target) }
    }

    fun addProvider(provider: TransformationContainerProvider<TRequest, TContainer, TEnum>) {
      providers.add(provider)
    }

    fun getProviderCount(): Int = providers.size
  }

  class CompositeOnTransformationChoice<TContainer : TransformationContainer<*>>(
    private val choices: MutableList<OnTransformationChoice<TContainer>>,
  ) : OnTransformationChoice<TContainer> {
    override fun isChosen(container: TContainer): Mono<Boolean> = choices.fold(true.toMono()) { acc, choice ->
      acc.filter { it }.flatMap { choice.isChosen(container) }.defaultIfEmpty(false)
    }

    fun addChoice(choice: OnTransformationChoice<TContainer>) {
      choices.add(choice)
    }

    fun getChoiceCount(): Int = choices.size
  }
}
