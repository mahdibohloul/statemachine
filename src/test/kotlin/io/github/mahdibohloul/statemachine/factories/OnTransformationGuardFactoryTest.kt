package io.github.mahdibohloul.statemachine.factories

import io.github.mahdibohloul.statemachine.StateMachineTestHelper
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
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

class OnTransformationGuardFactoryTest {
  enum class NotMatchEnum {
    Test,
  }

  class NotMatchContainer(
    override val source: NotMatchEnum?,
    override val target: NotMatchEnum?,
  ) : TransformationContainer<NotMatchEnum>

  class NotMatchTransformationGuard : OnTransformationGuard<NotMatchContainer> {
    override fun execute(container: NotMatchContainer): Mono<Boolean> = true.toMono()
  }

  @InjectMocks
  private lateinit var factory: OnTransformationGuardFactory

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
      StateMachineTestHelper.TrueTransformationGuard::class,
      StateMachineTestHelper.FalseTransformationGuard::class,
      NotMatchTransformationGuard::class,
    )

    val guards = listOf(
      StateMachineTestHelper.TrueTransformationGuard(),
      StateMachineTestHelper.FalseTransformationGuard(),
      NotMatchTransformationGuard(),
    )

    // when
    whenever(applicationContext.getBeansOfType(OnTransformationGuard::class.java)).thenReturn(
      guardClasses.map { it.simpleName!! }.zip(guards).toMap(),
    )

    val guard = factory.getGuard<NotMatchContainer>(
      NotMatchTransformationGuard::class,
      StateMachineTestHelper.TrueTransformationGuard::class,
    )

    // verify
    assert(guard is CompositeBehaviors.CompositeOnTransformationGuard<NotMatchContainer>)
  }
}
