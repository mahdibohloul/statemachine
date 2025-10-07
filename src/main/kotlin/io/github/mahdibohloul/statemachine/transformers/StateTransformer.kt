package io.github.mahdibohloul.statemachine.transformers

import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
import org.springframework.core.Ordered
import reactor.core.publisher.Mono

/**
 * Interface for implementing state transformers that handle the transformation of state machines
 * from one state to another or vice versa.
 * State transformers are responsible for orchestrating the complete transformation process including validation,
 * execution, and response generation.
 *
 * This interface extends [Ordered] to allow transformers to be executed in a specific sequence
 * when multiple transformers are applicable to the same transformation request.
 *
 * For convenience, you can use the [tapsi.delivery.infrastructure.statemachine.transformers.StateTransformerAdapter]
 * to implement this interface effectively,
 * as it provides default implementations and common functionality.
 *
 * @param TRequest The type of the transformation request that contains the input data and parameters
 * @param TResponse The type of the transformation response that will be returned after a successful transformation
 * @param TEnum The enum type representing the possible states in the state machine
 * @see tapsi.delivery.infrastructure.statemachine.transformers.StateTransformerAdapter
 * @see StateMachineStateFactory
 */
interface StateTransformer<TRequest : TransformationRequest, TResponse : Any, TEnum : Enum<*>> : Ordered {

  /**
   * Executes the complete transformation process for the given request.
   * This method orchestrates the entire transformation workflow including
   * - Container creation and validation
   * - Before-transformation guards and actions
   * - During-transformation actions
   * - After-transformation guards and actions
   * - Response generation
   * - Error handling
   *
   * The transformation follows a reactive pattern and returns a [Mono] that will emit
   * the transformation response or an error if the transformation fails.
   *
   * @param request The transformation request containing all necessary data and parameters
   * @return A [Mono] that emits the transformation response or an error
   * @throws io.github.mahdibohloul.statemachine.StateMachineException when transformation fails
   */
  fun transform(request: TRequest): Mono<TResponse>

  /**
   * Returns the state that this transformer is responsible for handling.
   * This state can be either the source state (for transitions from this state)
   * or the target state (for transitions to this state), depending on the transformer's purpose.
   *
   * The returned state is used by the state machine framework to:
   * - Route transformation requests to the appropriate transformer
   * - Validate state transitions
   * - Determine the execution order of transformers
   *
   * @return The state enum value that this transformer handles
   */
  fun getState(): TEnum

  /**
   * Validates whether this transformer can handle the given transformer identifier.
   * This method is used by the state machine framework to determine if a transformer
   * is applicable for a specific transformation scenario.
   *
   * The transformer identifier typically contains information about:
   * - The source and target states
   * - The transformation type (e.g., creation, modification, termination)
   * - Any additional context that might affect transformer selection
   *
   * **Important**: This method should throw an exception with detailed information about why
   * the transformer cannot handle the request.
   * The exception will be captured
   * and used to provide meaningful error messages when no suitable transformer is found.
   *
   * @param transformerIdentifier The identifier containing transformation context and parameters
   * @throws Exception when the transformer cannot handle the given identifier, with a detailed reason
   */
  fun canHandle(transformerIdentifier: StateMachineStateFactory.TransformerIdentifier)
}
