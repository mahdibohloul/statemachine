package io.github.mahdibohloul.statemachine.transformers

import box.tapsi.libs.utilities.castOrThrow
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
import io.github.mahdibohloul.statemachine.support.AfterTransformationConfigurer
import io.github.mahdibohloul.statemachine.support.BeforeTransformationConfigurer
import io.github.mahdibohloul.statemachine.support.DuringTransformationConfigurer
import io.github.mahdibohloul.statemachine.support.StateMachineConfigurer
import org.springframework.core.Ordered
import reactor.core.publisher.Mono

/**
 * Abstract adapter class that provides a template implementation for state transformers.
 * This class implements the [StateTransformer] interface and provides a structured approach
 * to building state machine transformations with configurable phases.
 *
 * The adapter follows the Template Method pattern, allowing subclasses to customize
 * the transformation behavior by overriding specific configuration methods while
 * maintaining a consistent transformation workflow.
 *
 * ## Transformation Phases
 * The transformation process is divided into three main phases:
 * 1. **Before Transformation**: Validation and pre-processing
 * 2. **During Transformation**: Main transformation logic
 * 3. **After Transformation**: Post-processing and cleanup
 *
 * ## Usage
 * Subclasses should override the configuration methods to customize the transformation:
 * - [configure] for state machine configuration
 * - [configure] for before-transformation setup
 * - [configure] for during-transformation setup
 * - [configure] for after-transformation setup
 *
 * @param TRequest The type of the transformation request that contains input data and parameters
 * @param TContainer The type of the transformation container that holds state and data during transformation
 * @param TResponse The type of the transformation response that will be returned after successful transformation
 * @param TEnum The enum type representing the possible states in the state machine
 * @see StateTransformer
 * @see BeforeTransformationConfigurer
 * @see DuringTransformationConfigurer
 * @see AfterTransformationConfigurer
 * @see StateMachineConfigurer
 */
abstract class StateTransformerAdapter<
  TRequest : TransformationRequest,
  TContainer : TransformationContainer<TEnum>,
  TResponse : Any,
  TEnum : Enum<*>,
  > : StateTransformer<TRequest, TResponse, TEnum> {

  /**
   * Returns the execution order for this transformer.
   * Default implementation returns [Ordered.LOWEST_PRECEDENCE], meaning this transformer
   * will be executed last when multiple transformers are applicable.
   *
   * Subclasses should override this method to specify a higher priority if needed.
   *
   * @return The execution order value
   */
  override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

  /**
   * Validates whether this transformer can handle the given transformer identifier.
   * The default implementation checks that the transformation request is not null.
   * Subclasses should override this method to add their specific validation logic.
   *
   * @param transformerIdentifier The identifier containing transformation context and parameters
   * @throws Exception when the transformer cannot handle the given identifier, with detailed reason
   */
  override fun canHandle(transformerIdentifier: StateMachineStateFactory.TransformerIdentifier) {
    requireNotNull(
      transformerIdentifier
        .getMetadata(StateMachineStateFactory.TransformerIdentifier.MetadataKey.TRANSFORMATION_REQUEST),
    ) {
      "Transformation request is null. Please provide a valid transformation request."
    }
  }

  /**
   * Executes the complete transformation process for the given request.
   * This method orchestrates the entire transformation workflow by:
   * 1. Creating and configuring all transformation phases
   * 2. Executing the transformation in the correct order
   * 3. Handling errors and providing appropriate responses
   *
   * The transformation follows a reactive pattern and returns a [Mono] that will emit
   * the transformation response or an error if the transformation fails.
   *
   * @param request The transformation request containing all necessary data and parameters
   * @return A [Mono] that emits the transformation response or an error
   * @throws io.github.mahdibohloul.statemachine.StateMachineException when transformation fails
   */
  override fun transform(request: TRequest): Mono<TResponse> = Mono.defer {
    val stateMachineConfigurer = StateMachineConfigurer<TRequest, TContainer, TResponse, TEnum>()
    val beforeTransformationConfigurer = BeforeTransformationConfigurer<TContainer, TEnum>()
    val afterTransformationConfigurer = AfterTransformationConfigurer<TContainer, TEnum>()
    val duringTransformationConfigurer = DuringTransformationConfigurer<TContainer, TEnum>()
    configure(stateMachineConfigurer)
    configure(beforeTransformationConfigurer)
    configure(afterTransformationConfigurer)
    configure(duringTransformationConfigurer)
    doTransform(
      stateMachineConfigurer,
      request,
      beforeTransformationConfigurer,
      duringTransformationConfigurer,
      afterTransformationConfigurer,
    )
  }

  /**
   * Configures the main state machine settings.
   * This method is called during the transformation setup phase and allows subclasses
   * to configure the overall state machine behavior including
   * - Error handlers
   * - Container and response providers
   * - Source and target states
   *
   * The default implementation does nothing. Subclasses should override this method
   * to provide their specific configuration.
   *
   * @param configurer The [StateMachineConfigurer] used to configure the state machine
   */
  protected open fun configure(configurer: StateMachineConfigurer<TRequest, TContainer, TResponse, TEnum>): Unit = Unit

  /**
   * Configures the before-transformation phase.
   * This method is called during the transformation setup phase and allows subclasses
   * to configure validation guards and pre-processing actions that should be executed
   * before the main transformation logic.
   *
   * The default implementation does nothing. Subclasses should override this method
   * to add their specific before-transformation logic.
   *
   * @param configurer The [BeforeTransformationConfigurer] used to configure pre-transformation behavior
   */
  protected open fun configure(configurer: BeforeTransformationConfigurer<TContainer, TEnum>): Unit = Unit

  /**
   * Configures the after-transformation phase.
   * This method is called during the transformation setup phase and allows subclasses
   * to configure post-processing actions and cleanup logic that should be executed
   * after the main transformation logic.
   *
   * The default implementation does nothing. Subclasses should override this method
   * to add their specific after-transformation logic.
   *
   * @param configurer The [AfterTransformationConfigurer] used to configure post-transformation behavior
   */
  protected open fun configure(configurer: AfterTransformationConfigurer<TContainer, TEnum>): Unit = Unit

  /**
   * Configures the during-transformation phase.
   * This method is called during the transformation setup phase and allows subclasses
   * to configure the main transformation actions that should be executed during
   * the core transformation logic.
   *
   * The default implementation does nothing. Subclasses should override this method
   * to add their specific during-transformation logic.
   *
   * @param configurer The [DuringTransformationConfigurer] used to configure the main transformation behavior
   */
  protected open fun configure(configurer: DuringTransformationConfigurer<TContainer, TEnum>): Unit = Unit

  /**
   * Extension function to safely extract and cast the transformation request from a transformer identifier.
   * This utility method provides type-safe access to the transformation request stored in the
   * transformer identifier's metadata.
   *
   * @param T The expected type of the transformation request
   * @return The cast transformation request
   * @throws ClassCastException if the stored request cannot be cast to the expected type
   * @throws NullPointerException if no transformation request is stored in the identifier
   */
  protected inline fun <reified T : TransformationRequest> StateMachineStateFactory
    .TransformerIdentifier.getTransformationRequest(): T = getMetadata(
    StateMachineStateFactory.TransformerIdentifier.MetadataKey.TRANSFORMATION_REQUEST,
  )!!.castOrThrow<T>()

  /**
   * Executes the actual transformation workflow.
   * This private method orchestrates the complete transformation process by:
   * 1. Validating state machine configuration
   * 2. Creating the transformation container
   * 3. Executing all transformation phases in sequence
   * 4. Generating the final response
   * 5. Handling any errors that occur during the process
   *
   * @param stateMachineConfigurer The configured state machine settings
   * @param request The transformation request
   * @param beforeTransformationConfigurer The configured before-transformation phase
   * @param duringTransformationConfigurer The configured during-transformation phase
   * @param afterTransformationConfigurer The configured after-transformation phase
   * @return A [Mono] that emits the transformation response or an error
   */
  private fun doTransform(
    stateMachineConfigurer: StateMachineConfigurer<TRequest, TContainer, TResponse, TEnum>,
    request: TRequest,
    beforeTransformationConfigurer: BeforeTransformationConfigurer<TContainer, TEnum>,
    duringTransformationConfigurer: DuringTransformationConfigurer<TContainer, TEnum>,
    afterTransformationConfigurer: AfterTransformationConfigurer<TContainer, TEnum>,
  ): Mono<TResponse> = stateMachineConfigurer.apply { this.validate() }
    .transformationContainerProvider.provideContainer(
      request = request,
      source = stateMachineConfigurer.sourceState,
      target = stateMachineConfigurer.targetState,
    )
    .flatMap(beforeTransformationConfigurer::transform)
    .flatMap(duringTransformationConfigurer::transform)
    .flatMap(afterTransformationConfigurer::transform)
    .flatMap { stateMachineConfigurer.transformationResponseProvider.provideResponse(request, it) }
    .onErrorResume { stateMachineConfigurer.onTransformationErrorHandler.onError(request, it) }
}
