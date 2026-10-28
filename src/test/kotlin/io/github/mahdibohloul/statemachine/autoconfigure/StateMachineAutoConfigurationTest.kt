package io.github.mahdibohloul.statemachine.autoconfigure

import box.tapsi.libs.utilities.time.TimeOperator
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.annotations.StateMachineState
import io.github.mahdibohloul.statemachine.factories.OnTransformationActionFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationChoiceFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationErrorHandlerFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationGuardFactory
import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
import io.github.mahdibohloul.statemachine.providers.TransformationContainerProvider
import io.github.mahdibohloul.statemachine.providers.TransformationResponseProvider
import io.github.mahdibohloul.statemachine.support.StateMachineConfigurer
import io.github.mahdibohloul.statemachine.transformers.StateTransformer
import io.github.mahdibohloul.statemachine.transformers.StateTransformerAdapter
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Starts a real Spring application context with the auto-configuration.
 * The build runs this test against each supported Spring Boot line (see `-PspringBootVersion`).
 */
class StateMachineAutoConfigurationTest {
  private val contextRunner = ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(StateMachineAutoConfiguration::class.java))

  @Test
  fun `should register the state machine factories`() {
    contextRunner.run { context ->
      context.getBean(OnTransformationActionFactory::class.java)
      context.getBean(OnTransformationChoiceFactory::class.java)
      context.getBean(OnTransformationErrorHandlerFactory::class.java)
      context.getBean(OnTransformationGuardFactory::class.java)
      context.getBean(StateMachineStateFactory::class.java)
    }
  }

  @Test
  fun `should start together with all auto-configurations on the classpath`() {
    ApplicationContextRunner()
      .withUserConfiguration(EnableAutoConfigurationConfiguration::class.java)
      .run { context ->
        assertNull(context.startupFailure)
        context.getBean(StateMachineStateFactory::class.java)
        // A bean from the auto-configuration of box.tapsi.libs:utilities-starter
        context.getBean(TimeOperator::class.java)
      }
  }

  @Test
  fun `should discover and execute a state transformer`() {
    contextRunner.withUserConfiguration(ApprovalConfiguration::class.java).run { context ->
      val request = ApprovalRequest(amount = 21)
      val transformer = context.getBean(StateMachineStateFactory::class.java)
        .getTransformer<ApprovalStatus, ApprovalRequest, Int>(
          StateMachineStateFactory.TransformerIdentifier(
            ApprovalStatus.Approved,
            request,
            Int::class.javaObjectType,
          ),
        )

      assertSame(context.getBean(ApprovalTransformer::class.java), transformer)
      transformer.transform(request)
        .test()
        .expectNext(42)
        .verifyComplete()
    }
  }

  enum class ApprovalStatus {
    Pending,
    Approved,
  }

  data class ApprovalRequest(val amount: Int) : TransformationRequest

  class ApprovalContainer(
    val amount: Int,
    override val source: ApprovalStatus?,
    override val target: ApprovalStatus?,
  ) : TransformationContainer<ApprovalStatus>

  @StateMachineState
  class ApprovalTransformer : StateTransformerAdapter<ApprovalRequest, ApprovalContainer, Int, ApprovalStatus>() {
    override fun getState(): ApprovalStatus = ApprovalStatus.Approved

    override fun configure(
      configurer: StateMachineConfigurer<ApprovalRequest, ApprovalContainer, Int, ApprovalStatus>,
    ) {
      configurer.apply {
        sourceState = ApprovalStatus.Pending
        targetState = ApprovalStatus.Approved
        transformationContainerProvider =
          object : TransformationContainerProvider<ApprovalRequest, ApprovalContainer, ApprovalStatus> {
            override fun provideContainer(
              request: ApprovalRequest,
              source: ApprovalStatus?,
              target: ApprovalStatus?,
            ): Mono<ApprovalContainer> = ApprovalContainer(request.amount, source, target).toMono()
          }
        transformationResponseProvider =
          object : TransformationResponseProvider<ApprovalRequest, ApprovalContainer, Int> {
            override fun provideResponse(
              request: ApprovalRequest,
              container: ApprovalContainer,
            ): Mono<Int> = (container.amount * 2).toMono()
          }
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  class EnableAutoConfigurationConfiguration

  @Configuration(proxyBeanMethods = false)
  class ApprovalConfiguration {
    @Bean
    fun approvalTransformer(): StateTransformer<ApprovalRequest, Int, ApprovalStatus> = ApprovalTransformer()
  }
}
