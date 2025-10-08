package io.github.mahdibohloul.statemachine

import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

object StateMachineTestHelper {
  enum class TestEnum {
    Test,
  }

  class TestContainer(
    var value: Int = 2,
    override val source: TestEnum? = TestEnum.Test,
    override val target: TestEnum? = TestEnum.Test,
  ) : TransformationContainer<TestEnum>

  class TestRequest : TransformationRequest

  class MultiplyByTwoTransformationAction : OnTransformationAction<TestContainer> {
    override fun execute(container: TestContainer): Mono<TestContainer> {
      container.value *= 2
      return container.toMono()
    }
  }

  class PowerOfTwoTransformationAction : OnTransformationAction<TestContainer> {
    override fun execute(container: TestContainer): Mono<TestContainer> {
      container.value *= container.value
      return container.toMono()
    }
  }

  class EmptyCompleteTransformationErrorHandler : OnTransformationErrorHandler<TestRequest, Int> {
    override fun onError(request: TestRequest, error: Throwable): Mono<Int> = Mono.empty()
  }

  class TrueTransformationGuard : OnTransformationGuard<TestContainer> {
    override fun execute(container: TestContainer): Mono<Boolean> = true.toMono()
  }

  class FalseTransformationGuard : OnTransformationGuard<TestContainer> {
    override fun execute(container: TestContainer): Mono<Boolean> = false.toMono()
  }

  class TrueTransformationChoice : OnTransformationChoice<TestContainer> {
    override fun isChosen(container: TestContainer): Mono<Boolean> = true.toMono()
  }

  class FalseTransformationChoice : OnTransformationChoice<TestContainer> {
    override fun isChosen(container: TestContainer): Mono<Boolean> = false.toMono()
  }

  class ResponseProvider : TransformationResponseProvider<TestRequest, TestContainer, Int> {
    override fun provideResponse(request: TestRequest, container: TestContainer): Mono<Int> = container.value.toMono()
  }

  class EmptyResponseProvider : TransformationResponseProvider<TestRequest, TestContainer, Int> {
    override fun provideResponse(request: TestRequest, container: TestContainer): Mono<Int> = Mono.empty()
  }

  class ContainerProvider : TransformationContainerProvider<TestRequest, TestContainer, TestEnum> {
    override fun provideContainer(
      request: TestRequest,
      source: TestEnum?,
      target: TestEnum?,
    ): Mono<TestContainer> = TestContainer().toMono()
  }

  class EmptyContainerProvider : TransformationContainerProvider<TestRequest, TestContainer, TestEnum> {
    override fun provideContainer(
      request: TestRequest,
      source: TestEnum?,
      target: TestEnum?,
    ): Mono<TestContainer> = Mono.empty()
  }
}
