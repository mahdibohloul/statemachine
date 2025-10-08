package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.StateMachineTestHelper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

class OperatorExtensionsKtTest {
  @Test
  fun `andThen on action should return composite action when called on non-composite action`() {
    // given
    val action = StateMachineTestHelper.MultiplyByTwoTransformationAction()
    val anotherAction = StateMachineTestHelper.MultiplyByTwoTransformationAction()

    // when
    val result = action andThen anotherAction

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationAction)
  }

  @Test
  fun `andThen on action should return composite action when called on composite action`() {
    // given
    val action = spy(
      CompositeBehaviors.CompositeOnTransformationAction(
        mutableListOf(StateMachineTestHelper.MultiplyByTwoTransformationAction()),
      ),
    )
    val anotherAction = StateMachineTestHelper.MultiplyByTwoTransformationAction()

    // when
    val result = action andThen anotherAction

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationAction && result.getActionCount() == 2)
    verify(action).addAction(anotherAction)
  }

  @Test
  fun `andThen on error handler should return composite error handler when called on non-composite error handler`() {
    // given
    val errorHandler = StateMachineTestHelper.EmptyCompleteTransformationErrorHandler()
    val anotherErrorHandler =
      DefaultBehaviors.DefaultOnTransformationErrorHandler<StateMachineTestHelper.TestRequest, Int>()

    // when
    val result = errorHandler andThen anotherErrorHandler

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationErrorHandler)
  }

  @Test
  fun `andThen on error handler should return composite error handler when called on composite error handler`() {
    // given
    val errorHandler = spy(
      CompositeBehaviors.CompositeOnTransformationErrorHandler(
        mutableListOf(StateMachineTestHelper.EmptyCompleteTransformationErrorHandler()),
      ),
    )
    val anotherErrorHandler =
      DefaultBehaviors.DefaultOnTransformationErrorHandler<StateMachineTestHelper.TestRequest, Int>()

    // when
    val result = errorHandler andThen anotherErrorHandler

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationErrorHandler && result.getErrorHandlerCount() == 2)
    verify(errorHandler).addErrorHandler(anotherErrorHandler)
  }

  @Test
  fun `andThen on guard should return composite guard when called on non-composite guard`() {
    // given
    val guard = StateMachineTestHelper.TrueTransformationGuard()
    val anotherGuard = StateMachineTestHelper.TrueTransformationGuard()

    // when
    val result = guard andThen anotherGuard

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationGuard)
  }

  @Test
  fun `andThen on guard should return composite guard when called on composite guard`() {
    // given
    val guard = spy(
      CompositeBehaviors.CompositeOnTransformationGuard(
        mutableListOf(StateMachineTestHelper.TrueTransformationGuard()),
      ),
    )
    val anotherGuard = StateMachineTestHelper.TrueTransformationGuard()

    // when
    val result = guard andThen anotherGuard

    // verify
    assert(result is CompositeBehaviors.CompositeOnTransformationGuard && result.getGuardCount() == 2)
    verify(guard).addGuard(anotherGuard)
  }

  @Test
  fun `andThen on response provider should return composite provider when called on non-composite response provider`() {
    // given
    val provider = StateMachineTestHelper.ResponseProvider()
    val anotherProvider = StateMachineTestHelper.EmptyResponseProvider()

    // when
    val result = provider andThen anotherProvider

    // verify
    assert(result is CompositeBehaviors.CompositeTransformationResponseProvider)
  }

  @Test
  fun `andThen on response provider should return composite provider when called on composite response provider`() {
    // given
    val provider = spy(
      CompositeBehaviors.CompositeTransformationResponseProvider(
        mutableListOf(StateMachineTestHelper.ResponseProvider()),
      ),
    )
    val anotherProvider = StateMachineTestHelper.EmptyResponseProvider()

    // when
    val result = provider andThen anotherProvider

    // verify
    assert(result is CompositeBehaviors.CompositeTransformationResponseProvider && result.getProviderCount() == 2)
    verify(provider).addProvider(anotherProvider)
  }

  @Test
  fun `andThen on container provider should return composite provider when called on non-composite provider`() {
    // given
    val provider = StateMachineTestHelper.ContainerProvider()
    val anotherProvider = StateMachineTestHelper.EmptyContainerProvider()

    // when
    val result = provider andThen anotherProvider

    // verify
    assert(result is CompositeBehaviors.CompositeTransformationContainerProvider)
  }

  @Test
  fun `andThen on container provider should return composite provider when called on composite provider`() {
    // given
    val provider = spy(
      CompositeBehaviors.CompositeTransformationContainerProvider(
        mutableListOf(StateMachineTestHelper.ContainerProvider()),
      ),
    )
    val anotherProvider = StateMachineTestHelper.EmptyContainerProvider()

    // when
    val result = provider andThen anotherProvider

    // verify
    assert(result is CompositeBehaviors.CompositeTransformationContainerProvider && result.getProviderCount() == 2)
    verify(provider).addProvider(anotherProvider)
  }
}
