package io.github.mahdibohloul.statemachine.factories

import io.github.mahdibohloul.statemachine.StateMachineTestHelper
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.slf4j.Logger
import org.springframework.context.ApplicationContext
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class OnTransformationActionFactoryTest {
  enum class NotMatchEnum {
    Test,
  }

  class NotMatchContainer(
    override val source: NotMatchEnum?,
    override val target: NotMatchEnum?,
  ) : TransformationContainer<NotMatchEnum>

  class NotMatchTransformationAction : OnTransformationAction<NotMatchContainer> {
    override fun execute(container: NotMatchContainer): Mono<NotMatchContainer> = container.toMono()
  }

  @InjectMocks
  private lateinit var factory: OnTransformationActionFactory

  @Mock
  @Suppress("detekt.UnusedPrivateProperty")
  private lateinit var logger: Logger

  @Mock
  private lateinit var applicationContext: ApplicationContext

  @BeforeEach
  fun init() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `should ignore guard if the type is not match`() {
    // given
    val guardClasses = listOf(
      StateMachineTestHelper.PowerOfTwoTransformationAction::class,
      StateMachineTestHelper.MultiplyByTwoTransformationAction::class,
      NotMatchTransformationAction::class,
    )

    val guards = listOf(
      StateMachineTestHelper.PowerOfTwoTransformationAction(),
      StateMachineTestHelper.MultiplyByTwoTransformationAction(),
      NotMatchTransformationAction(),
    )

    // when
    whenever(applicationContext.getBeansOfType(OnTransformationAction::class.java)).thenReturn(
      guardClasses.map { it.simpleName!! }.zip(guards).toMap(),
    )

    val guard = factory.getAction<NotMatchContainer>(
      NotMatchTransformationAction::class,
      StateMachineTestHelper.PowerOfTwoTransformationAction::class,
    )

    // verify
    assert(guard is CompositeBehaviors.CompositeOnTransformationAction<NotMatchContainer>)
  }
}
