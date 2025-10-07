package io.github.mahdibohloul.statemachine.support

import io.github.mahdibohloul.statemachine.StateMachineException
import io.github.mahdibohloul.statemachine.StateMachineTestHelper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import reactor.kotlin.test.test

class CompositeBehaviorsTest {
  @Test
  fun `should composite two transformation actions with each other`() {
    // given
    val container = StateMachineTestHelper.TestContainer(value = 2)
    val multiplyByTwoTransformationAction = spy(StateMachineTestHelper.MultiplyByTwoTransformationAction())
    val powerOfTwoTransformationAction = spy(StateMachineTestHelper.PowerOfTwoTransformationAction())
    val compositeOnTransformationAction = CompositeBehaviors.CompositeOnTransformationAction(
      mutableListOf(multiplyByTwoTransformationAction, powerOfTwoTransformationAction),
    )

    // when
    compositeOnTransformationAction.execute(container)
      .test()
      .expectNextMatches { it.value == 16 }
      .verifyComplete()

    // verify
    verify(multiplyByTwoTransformationAction, times(1)).execute(container)
    verify(powerOfTwoTransformationAction, times(1)).execute(container)
  }

  @Test
  fun `should chain the error handlers and return an empty mono`() {
    // given
    val defaultBehavior =
      spy(DefaultBehaviors.DefaultOnTransformationErrorHandler<StateMachineTestHelper.TestRequest, Int>())
    val emptyCompleteTransformationErrorHandler = spy(StateMachineTestHelper.EmptyCompleteTransformationErrorHandler())
    val compositeOnTransformationErrorHandler = CompositeBehaviors.CompositeOnTransformationErrorHandler(
      mutableListOf(emptyCompleteTransformationErrorHandler, defaultBehavior),
    )
    val request = StateMachineTestHelper.TestRequest()
    val error = Throwable()

    // when
    compositeOnTransformationErrorHandler.onError(request, error)
      .test()
      .verifyComplete()

    // verify
    verify(emptyCompleteTransformationErrorHandler, times(1)).onError(request, error)
    verifyNoInteractions(defaultBehavior)
  }

  @Test
  fun `should return true when all guards returns true`() {
    // given
    val trueGuard = spy(StateMachineTestHelper.TrueTransformationGuard())
    val compositeOnTransformationGuard = CompositeBehaviors.CompositeOnTransformationGuard(
      mutableListOf(trueGuard, trueGuard, trueGuard),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationGuard.execute(container)
      .test()
      .expectNext(true)
      .verifyComplete()

    // verify
    verify(trueGuard, times(3)).execute(container)
  }

  @Test
  fun `should fail-fast if an earlier guard fails sooner`() {
    // given
    val trueGuard = spy(StateMachineTestHelper.TrueTransformationGuard())
    val falseGuard = spy(StateMachineTestHelper.FalseTransformationGuard())
    val compositeOnTransformationGuard = CompositeBehaviors.CompositeOnTransformationGuard(
      mutableListOf(trueGuard, falseGuard, trueGuard, trueGuard),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationGuard.execute(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(trueGuard, times(1)).execute(container)
    verify(falseGuard, times(1)).execute(container)
  }

  @Test
  fun `should return the result of the last guard when guards are in chain`() {
    // given
    val trueGuard = spy(StateMachineTestHelper.TrueTransformationGuard())
    val falseGuard = spy(StateMachineTestHelper.FalseTransformationGuard())
    val compositeOnTransformationGuard = CompositeBehaviors.CompositeOnTransformationGuard(
      mutableListOf(trueGuard, falseGuard),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationGuard.execute(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(trueGuard, times(1)).execute(container)
    verify(falseGuard, times(1)).execute(container)
  }

  @Test
  fun `should throw exception when validating the guard fails`() {
    // given
    val trueGuard = spy(StateMachineTestHelper.TrueTransformationGuard())
    val falseGuard = spy(StateMachineTestHelper.FalseTransformationGuard())
    val compositeOnTransformationGuard = CompositeBehaviors.CompositeOnTransformationGuard(
      mutableListOf(trueGuard, falseGuard, trueGuard),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationGuard.validate(container)
      .test()
      .expectError(StateMachineException.GuardValidationException::class.java)
      .verify()

    // verify
    verify(trueGuard, times(1)).validate(container)
    verify(falseGuard, times(1)).validate(container)
  }

  @Test
  fun `should return true when all validations are successful`() {
    // given
    val trueGuard = spy(StateMachineTestHelper.TrueTransformationGuard())
    val compositeOnTransformationGuard = CompositeBehaviors.CompositeOnTransformationGuard(
      mutableListOf(trueGuard, trueGuard, trueGuard),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationGuard.validate(container)
      .test()
      .expectNext(true)
      .verifyComplete()

    // verify
    verify(trueGuard, times(3)).validate(container)
  }

  @Test
  fun `should return the response of the first provider that returns a response`() {
    // given
    val responseProvider = spy(StateMachineTestHelper.ResponseProvider())
    val emptyResponseProvider = spy(StateMachineTestHelper.EmptyResponseProvider())
    val compositeTransformationResponseProvider =
      CompositeBehaviors.CompositeTransformationResponseProvider(
        mutableListOf(emptyResponseProvider, responseProvider),
      )
    val request = StateMachineTestHelper.TestRequest()
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeTransformationResponseProvider.provideResponse(request, container)
      .test()
      .expectNext(2)
      .verifyComplete()

    // verify
    verify(emptyResponseProvider, times(1)).provideResponse(request, container)
    verify(responseProvider, times(1)).provideResponse(request, container)
  }

  @Test
  fun `should ignore the rest of the providers when a provider returns a response`() {
    // given
    val responseProvider = spy(StateMachineTestHelper.ResponseProvider())
    val emptyResponseProvider = spy(StateMachineTestHelper.EmptyResponseProvider())
    val compositeTransformationResponseProvider =
      CompositeBehaviors.CompositeTransformationResponseProvider(
        mutableListOf(responseProvider, emptyResponseProvider),
      )
    val request = StateMachineTestHelper.TestRequest()
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeTransformationResponseProvider.provideResponse(request, container)
      .test()
      .expectNext(2)
      .verifyComplete()

    // verify
    verify(responseProvider, times(1)).provideResponse(request, container)
    verifyNoInteractions(emptyResponseProvider)
  }

  @Test
  fun `should return the container of the first provider that returns a container`() {
    // given
    val containerProvider = spy(StateMachineTestHelper.ContainerProvider())
    val emptyContainerProvider = spy(StateMachineTestHelper.EmptyContainerProvider())
    val compositeTransformationContainerProvider =
      CompositeBehaviors.CompositeTransformationContainerProvider(
        mutableListOf(emptyContainerProvider, containerProvider),
      )
    val request = StateMachineTestHelper.TestRequest()
    val source = StateMachineTestHelper.TestEnum.Test
    val target = StateMachineTestHelper.TestEnum.Test

    // when
    compositeTransformationContainerProvider.provideContainer(request, source, target)
      .test()
      .expectNextMatches { it.value == 2 }
      .verifyComplete()

    // verify
    verify(emptyContainerProvider, times(1)).provideContainer(request, source, target)
    verify(containerProvider, times(1)).provideContainer(request, source, target)
  }

  @Test
  fun `should ignore the rest of the providers when a provider returns a container`() {
    // given
    val containerProvider = spy(StateMachineTestHelper.ContainerProvider())
    val emptyContainerProvider = spy(StateMachineTestHelper.EmptyContainerProvider())
    val compositeTransformationContainerProvider =
      CompositeBehaviors.CompositeTransformationContainerProvider(
        mutableListOf(containerProvider, emptyContainerProvider),
      )
    val request = StateMachineTestHelper.TestRequest()
    val source = StateMachineTestHelper.TestEnum.Test
    val target = StateMachineTestHelper.TestEnum.Test

    // when
    compositeTransformationContainerProvider.provideContainer(request, source, target)
      .test()
      .expectNextMatches { it.value == 2 }
      .verifyComplete()

    // verify
    verify(containerProvider, times(1)).provideContainer(request, source, target)
    verifyNoInteractions(emptyContainerProvider)
  }

  @Test
  fun `should return true when all choices return true`() {
    // given
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice, trueChoice, trueChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(true)
      .verifyComplete()

    // verify
    verify(trueChoice, times(3)).isChosen(container)
  }

  @Test
  fun `should fail-fast if an earlier choice returns false`() {
    // given
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val falseChoice = spy(StateMachineTestHelper.FalseTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice, falseChoice, trueChoice, trueChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(trueChoice, times(1)).isChosen(container)
    verify(falseChoice, times(1)).isChosen(container)
  }

  @Test
  fun `should return false when any choice returns false`() {
    // given
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val falseChoice = spy(StateMachineTestHelper.FalseTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice, falseChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(trueChoice, times(1)).isChosen(container)
    verify(falseChoice, times(1)).isChosen(container)
  }

  @Test
  fun `should return false when first choice returns false`() {
    // given
    val falseChoice = spy(StateMachineTestHelper.FalseTransformationChoice())
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(falseChoice, trueChoice, trueChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(falseChoice, times(1)).isChosen(container)
    verifyNoInteractions(trueChoice)
  }

  @Test
  fun `should return true when only one choice exists and it returns true`() {
    // given
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(true)
      .verifyComplete()

    // verify
    verify(trueChoice, times(1)).isChosen(container)
  }

  @Test
  fun `should return false when only one choice exists and it returns false`() {
    // given
    val falseChoice = spy(StateMachineTestHelper.FalseTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(falseChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(falseChoice, times(1)).isChosen(container)
  }

  @Test
  fun `should return true when no choices exist`() {
    // given
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice<
      StateMachineTestHelper.TestContainer,
      >(mutableListOf())

    val container = StateMachineTestHelper.TestContainer()

    // when

    // verify
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(true)
      .verifyComplete()
  }

  @Test
  fun `should add choice to composite and evaluate it`() {
    // given
    val trueChoice = spy(StateMachineTestHelper.TrueTransformationChoice())
    val falseChoice = spy(StateMachineTestHelper.FalseTransformationChoice())
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice),
    )
    val container = StateMachineTestHelper.TestContainer()

    // when
    compositeOnTransformationChoice.addChoice(falseChoice)
    compositeOnTransformationChoice.isChosen(container)
      .test()
      .expectNext(false)
      .verifyComplete()

    // verify
    verify(trueChoice, times(1)).isChosen(container)
    verify(falseChoice, times(1)).isChosen(container)
  }

  @Test
  fun `should return correct choice count`() {
    // given
    val trueChoice = StateMachineTestHelper.TrueTransformationChoice()
    val falseChoice = StateMachineTestHelper.FalseTransformationChoice()
    val compositeOnTransformationChoice = CompositeBehaviors.CompositeOnTransformationChoice(
      mutableListOf(trueChoice, falseChoice),
    )

    // when

    // verify
    assert(compositeOnTransformationChoice.getChoiceCount() == 2)
    compositeOnTransformationChoice.addChoice(trueChoice)
    assert(compositeOnTransformationChoice.getChoiceCount() == 3)
  }
}
