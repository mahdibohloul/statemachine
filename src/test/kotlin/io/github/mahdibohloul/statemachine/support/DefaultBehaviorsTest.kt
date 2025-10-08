package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.StateMachineException
import io.github.mahdibohloul.statemachine.StateMachineTestHelper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.verifyNoInteractions
import reactor.kotlin.test.test

class DefaultBehaviorsTest {
  @Test
  fun `the default on transformation action should pass the container through`() {
    // given
    val container = spy(StateMachineTestHelper.TestContainer())
    val defaultOnTransformationAction =
      DefaultBehaviors.DefaultOnTransformationAction<StateMachineTestHelper.TestContainer>()

    // when
    defaultOnTransformationAction.execute(container)
      .test()
      .expectNext(container)
      .verifyComplete()

    // verify
    verifyNoInteractions(container)
  }

  @Test
  fun `the default on transformation error handler should return the error`() {
    // given
    val error = spy(RuntimeException("Test"))
    val request = spy(StateMachineTestHelper.TestRequest())
    val defaultOnTransformationErrorHandler =
      DefaultBehaviors.DefaultOnTransformationErrorHandler<StateMachineTestHelper.TestRequest, Any>()

    // when
    defaultOnTransformationErrorHandler.onError(request, error)
      .test()
      .verifyErrorMatches { it is RuntimeException && it.message == "Test" }

    // verify
    verifyNoInteractions(request)
  }

  @Test
  fun `the default on transformation guard should return true`() {
    // given
    val container = spy(StateMachineTestHelper.TestContainer())
    val defaultOnTransformationGuard =
      DefaultBehaviors.DefaultOnTransformationGuard<StateMachineTestHelper.TestContainer>()

    // when
    defaultOnTransformationGuard.execute(container)
      .test()
      .expectNext(true)
      .verifyComplete()

    // verify
    verifyNoInteractions(container)
  }

  @Test
  fun `the default transformation response provider should return an empty mono`() {
    // given
    val request = spy(StateMachineTestHelper.TestRequest())
    val container = spy(StateMachineTestHelper.TestContainer())
    val defaultTransformationResponseProvider = DefaultBehaviors
      .DefaultTransformationResponseProvider<
        StateMachineTestHelper.TestRequest,
        StateMachineTestHelper.TestContainer,
        Any,
        >()

    // when
    defaultTransformationResponseProvider.provideResponse(request, container)
      .test()
      .verifyComplete()

    // verify
    verifyNoInteractions(request)
    verifyNoInteractions(container)
  }

  @Test
  fun `the default transformation container provider should return an error mono`() {
    // given
    val request = spy(StateMachineTestHelper.TestRequest())
    val defaultTransformationContainerProvider = DefaultBehaviors
      .DefaultTransformationContainerProvider<
        StateMachineTestHelper.TestRequest,
        StateMachineTestHelper.TestContainer,
        StateMachineTestHelper.TestEnum,
        >()

    // when
    defaultTransformationContainerProvider.provideContainer(
      request,
      StateMachineTestHelper.TestEnum.Test,
      StateMachineTestHelper.TestEnum.Test,
    )
      .test()
      .verifyErrorMatches { it is StateMachineException.NoContainerProviderConfiguredException }

    // verify
    verifyNoInteractions(request)
  }
}
