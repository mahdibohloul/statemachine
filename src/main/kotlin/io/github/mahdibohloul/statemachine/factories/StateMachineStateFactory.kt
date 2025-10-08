package io.github.mahdibohloul.statemachine.factories

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.annotations.StateMachineState
import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory.TransformerIdentifier
import io.github.mahdibohloul.statemachine.support.asFailure
import io.github.mahdibohloul.statemachine.support.checkAsResult
import io.github.mahdibohloul.statemachine.support.isSuccess
import io.github.mahdibohloul.statemachine.transformers.StateTransformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.OrderComparator
import org.springframework.core.ResolvableType
import kotlin.reflect.KClass

/**
 * Factory class responsible for discovering, registering, and retrieving state transformers.
 * This component serves as the central registry for all state machine transformers in the application.
 *
 * ## Responsibilities
 * - **Discovery**: Automatically discovers all beans annotated with [@StateMachineState]
 * - **Registration**: Registers transformers based on their state and request/response types
 * - **Retrieval**: Provides transformers that can handle specific transformation scenarios
 * - **Ordering**: Ensures transformers are executed in the correct order based on their priority
 *
 * ## Initialization Process
 * The factory initializes automatically when the Spring context is refreshed:
 * 1. Scans for all beans with [@StateMachineState] annotation
 * 2. Extract type information using Spring's [ResolvableType]
 * 3. Creates [TransformerIdentifier] instances for each transformer
 * 4. Registers transformers in an internal map for efficient lookup
 *
 * ## Transformer Selection
 * When retrieving a transformer for a specific scenario:
 * 1. Find all registered transformers for the given identifier
 * 2. Filters transformers that can handle the scenario (using [canHandle])
 * 3. Sorts transformers by their execution order
 * 4. Returns the first applicable transformer
 *
 * @param applicationContext Spring application context for bean discovery
 * @param logger Logger for initialization and error reporting
 * @see StateTransformer
 * @see StateMachineState
 * @see TransformerIdentifier
 */
class StateMachineStateFactory(
  private val applicationContext: ApplicationContext,
) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  private var isInitialized: Boolean = false
  private val transformerMap: MutableMap<
    TransformerIdentifier,
    MutableList<StateTransformer<*, *, *>>,
    > = mutableMapOf()

  /**
   * Retrieves a state transformer that can handle the specified transformation scenario.
   * This method finds all registered transformers for the given identifier, filters them
   * based on their capability to handle the scenario, and returns the first applicable
   * transformer sorted by execution order.
   *
   * @param transformerIdentifier The identifier containing state and type information
   * @return A transformer that can handle the specified scenario
   * @throws IllegalStateException if no suitable transformer is found
   * @throws ClassCastException if the found transformer cannot be cast to the expected type
   */
  fun <TEnum : Enum<*>, TRequest : TransformationRequest, TResponse : Any> getTransformer(
    transformerIdentifier: TransformerIdentifier,
  ): StateTransformer<TRequest, TResponse, TEnum> {
    init()
    val transformers = getTransformers(transformerIdentifier)
    val supportedTransformers = getSortedSupportedTransformers(transformers, transformerIdentifier)

    return requireNotNull(supportedTransformers.first() as? StateTransformer<TRequest, TResponse, TEnum>) {
      "Cannot cast state transformer ${supportedTransformers.first()::class.getOriginalClass().simpleName} for " +
        "transformer identifier $transformerIdentifier"
    }
  }

  /**
   * Initializes the factory by discovering and registering all state transformers.
   * This method is automatically called when the Spring context is refreshed and
   * performs the following operations:
   * 1. Discover all beans annotated with [@StateMachineState]
   * 2. Extract type information using Spring's reflection utilities
   * 3. Creates [TransformerIdentifier] instances for each transformer
   * 4. Registers transformers in the internal map for efficient lookup
   *
   * The initialization is thread-safe and idempotent - subsequent calls will be ignored
   * if the factory is already initialized.
   *
   * @param event The context refreshed event (unused, required by @EventListener)
   */
  @EventListener(ContextRefreshedEvent::class)
  fun init() {
    synchronized(this) {
      if (isInitialized) {
        return
      }
      logger.info("Initializing state machine state factory")
      val stateMachineStates = applicationContext.getBeansWithAnnotation(StateMachineState::class.java)
      stateMachineStates.entries.forEach { (beanName, bean) ->
        val stateTransformer = bean as StateTransformer<*, *, *>
        ResolvableType.forClass(stateTransformer::class.java)
          .`as`(StateTransformer::class.java)
          .let { resolvedTransformer ->
            val requestType = resolvedTransformer.generics[0].toClass() as Class<out TransformationRequest>
            val responseType = resolvedTransformer.generics[1]

            val identifier = TransformerIdentifier.fromTransformer(
              stateTransformer.getState(),
              requestType.kotlin,
              responseType,
            )

            transformerMap.computeIfAbsent(identifier, { _ -> mutableListOf() }).add(stateTransformer)
            logger.info(
              "State transformer $beanName with order ${stateTransformer.order} " +
                "registered for transformer identifier: $identifier",
            )
          }
      }
      logger.info("Discovered ${transformerMap.size} state transformers")
      logger.info("State machine state factory initialized")
      isInitialized = true
    }
  }

  /**
   * Filters and sorts transformers based on their capability to handle the given scenario.
   * This method evaluates each transformer's [canHandle] method and returns only those
   * that can successfully handle the transformation scenario, sorted by their execution order.
   *
   * @param transformers List of transformers to evaluate
   * @param transformerIdentifier The scenario identifier to test against
   * @return List of supported transformers sorted by execution order
   * @throws Exception if no transformers can handle the scenario (throws the first transformer's error)
   */
  private fun getSortedSupportedTransformers(
    transformers: MutableList<StateTransformer<*, *, *>>,
    transformerIdentifier: TransformerIdentifier,
  ): List<StateTransformer<*, *, *>> {
    val withSupportedResult = transformers.map { t -> t to t.checkAsResult(transformerIdentifier) }

    if (withSupportedResult.all { (_, supportResult) -> !supportResult.isSuccess() }) {
      val firstSupportResult = withSupportedResult.first().second
      throw firstSupportResult.asFailure().error
    }

    val supportedTransformers = withSupportedResult.filter { (_, res) -> res.isSuccess() }
      .map { (t, _) -> t }
      .sortedWith(OrderComparator.INSTANCE)

    return supportedTransformers
  }

  /**
   * Retrieves all registered transformers for the given identifier.
   * This method looks up transformers in the internal registry and ensures
   * that at least one transformer is available for the specified identifier.
   *
   * @param transformerIdentifier The identifier to look up transformers for
   * @return List of registered transformers for the identifier
   * @throws IllegalStateException if no transformers are registered for the identifier
   */
  private fun getTransformers(transformerIdentifier: TransformerIdentifier): MutableList<StateTransformer<*, *, *>> {
    val transformers = requireNotNull(transformerMap[transformerIdentifier]) {
      "No state transformer found for state with identifier $transformerIdentifier"
    }

    check(transformers.isNotEmpty()) {
      "No state transformer found for state with identifier $transformerIdentifier"
    }
    return transformers
  }

  /**
   * Identifier class that uniquely identifies a transformation scenario.
   * This class combines state information with request and response type information
   * to create a unique key for transformer registration and lookup.
   *
   * ## Key Components
   * - **State**: The enum value representing the state (source or target)
   * - **Request Type**: The type of transformation request
   * - **Response Type**: The type of transformation response (including generic parameters)
   * - **Metadata**: Additional context information for transformer selection
   *
   * ## Usage
   * Transformer identifiers are used to:
   * - Register transformers in the factory's internal map
   * - Look up transformers for specific transformation scenarios
   * - Provide context information to transformers during capability checks
   *
   * @param state The enum value representing the state
   * @param requestType The type of transformation request
   * @param responseTypeRaw The raw response type class
   * @param responseTypeGenerics Generic type parameters for the response type
   * @param metadata Additional context information for transformer selection
   */
  class TransformerIdentifier private constructor(
    private val state: Enum<*>,
    private val requestType: KClass<out TransformationRequest>,
    private val responseTypeRaw: Class<*>,
    private val responseTypeGenerics: List<Class<*>>? = null,
    /**
     * Metadata map to store additional information about the transformer.
     * This field will be mainly used to select between different transformers
     * Each transformer can choose to whether to support a transformer identifier or not.
     */
    private val metadata: MutableMap<String, Any> = mutableMapOf(),
  ) {

    /**
     * Secondary constructor that creates an identifier from a concrete request instance.
     * This constructor automatically extracts the request type and stores the request
     * in the metadata for later use by transformers.
     *
     * @param state The enum value representing the state
     * @param request The concrete transformation request instance
     * @param responseTypeRaw The raw response type class
     * @param responseTypeGenerics Generic type parameters for the response type
     */
    constructor(
      state: Enum<*>,
      request: TransformationRequest,
      responseTypeRaw: Class<*>,
      responseTypeGenerics: List<Class<*>>? = null,
    ) : this(
      state,
      requestType = request::class,
      responseTypeRaw = responseTypeRaw,
      responseTypeGenerics = responseTypeGenerics,
      metadata = mutableMapOf(MetadataKey.TRANSFORMATION_REQUEST to request),
    )

    /**
     * Adds or updates metadata with the specified key and value.
     *
     * @param key The metadata key
     * @param value The metadata value
     * @return The previous value associated with the key, or null if the key was not present
     */
    fun withMetadata(key: String, value: Any): Any? = metadata.put(key, value)

    /**
     * Retrieves metadata associated with the specified key.
     *
     * @param key The metadata key
     * @return The metadata value, or null if the key is not present
     */
    fun getMetadata(key: String): Any? = metadata[key]

    /**
     * Checks whether metadata exists for the specified key.
     *
     * @param key The metadata key to check
     * @return true if metadata exists for the key, false otherwise
     */
    fun hasMetadata(key: String): Boolean = metadata.containsKey(key)

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is TransformerIdentifier) return false

      if (state != other.state) return false
      if (requestType != other.requestType) return false
      if (responseTypeRaw != other.responseTypeRaw) return false
      if (responseTypeGenerics != other.responseTypeGenerics) return false

      return true
    }

    override fun hashCode(): Int {
      var result = state.hashCode()
      result = 31 * result + requestType.hashCode()
      result = 31 * result + responseTypeRaw.hashCode()
      result = 31 * result + (responseTypeGenerics?.hashCode() ?: 0)
      return result
    }

    override fun toString(): String = buildString {
      append("TransformerIdentifier(state=")
      append(state)
      append(", requestType=")
      append(requestType)
      append(", responseTypeRaw=")
      append(responseTypeRaw)
      append(", responseTypeGenerics=")
      append(responseTypeGenerics)
      append(")")
    }

    /**
     * Constants for commonly used metadata keys.
     */
    object MetadataKey {
      /**
       * Key for storing the transformation request in metadata.
       */
      const val TRANSFORMATION_REQUEST = "transformationRequest"
    }

    companion object {
      /**
       * Creates a transformer identifier from a transformer's type information.
       * This factory method extracts type information from a [ResolvableType] and
       * creates an appropriate identifier instance.
       *
       * @param state The enum value representing the state
       * @param requestType The type of transformation request
       * @param responseType The resolvable type containing response type information
       * @return A transformer identifier instance
       */
      fun fromTransformer(
        state: Enum<*>,
        requestType: KClass<out TransformationRequest>,
        responseType: ResolvableType,
      ): TransformerIdentifier {
        val rawClass = responseType.toClass()
        if (responseType.hasGenerics()) {
          return TransformerIdentifier(state, requestType, rawClass, responseType.generics.map { it.toClass() })
        }
        return TransformerIdentifier(state, requestType, rawClass)
      }
    }
  }
}
