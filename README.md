## statemachine

Lightweight, reactive state-machine building blocks for Kotlin and Spring.

This library helps you implement robust, composable state transitions with Project Reactor, providing a clear separation
of concerns across three phases: before, during, and after transformation.

### Why this library?

- **Reactive-first**: Built on Reactor `Mono` for non-blocking flows.
- **Composable**: Chain actions, guards, and error handlers with simple operators.
- **Type-safe**: Generics enforce correct container/state usage.
- **Spring-friendly**: Factories and annotations integrate with Spring's `ApplicationContext`.
- **Transaction-aware**: Execute after-commit actions only when a transaction successfully commits.
- **Production-ready**: Used in production at Tapsi for complex delivery request state management.

### At a glance

```kotlin
@StateMachineState
class OrderProcessingTransformer(
  private val containerProvider: OrderContainerProvider,
  private val responseProvider: OrderResponseProvider,
  private val actionFactory: OnTransformationActionFactory,
  private val guardFactory: OnTransformationGuardFactory,
) : StateTransformerAdapter<OrderRequest, OrderContainer, Order, OrderStatus>() {
  
  override fun getState(): OrderStatus = OrderStatus.Processing
  override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

  override fun configure(configurer: StateMachineConfigurer<OrderRequest, OrderContainer, Order, OrderStatus>) {
    configurer.apply {
      sourceState = OrderStatus.Created
      targetState = OrderStatus.Processing
      transformationContainerProvider = containerProvider
      transformationResponseProvider = responseProvider
      onTransformationErrorHandler = OrderErrorHandler()
    }
  }

  override fun configure(configurer: BeforeTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      beforeTransformationGuard = guardFactory.getGuard(
        PaymentValidationGuard::class,
        InventoryCheckGuard::class
      )
      beforeTransformationAction = actionFactory.getAction(
        EnrichOrderAction::class,
        ReserveInventoryAction::class
      )
    }
  }

  override fun configure(configurer: DuringTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      duringTransformationAction = actionFactory.getAction(
        ProcessPaymentAction::class,
        SaveOrderAction::class
      )
    }
  }

  override fun configure(configurer: AfterTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      afterTransformationAction = actionFactory.getAction(
        SendConfirmationAction::class
      )
      afterCommitTransactionAction = actionFactory.getAction(
        NotifyWarehouseAction::class
      )
    }
  }
}
```

---

## Installation

Available as a Maven artifact.

Maven:

```xml

<dependency>
    <groupId>io.github.mahdibohloul</groupId>
    <artifactId>statemachine</artifactId>
    <version>0.10.0</version>
</dependency>
```

Minimums:

- Kotlin 1.9+
- Java 21+
- Spring 6.1+/Boot 3.3+ (optional, for Spring integration)
- Reactor 3.7+

---

## Core Concepts

### Basic Types
- **States (`TEnum : Enum<*>`)**: Your domain states (e.g., `Created`, `Processing`, `Completed`).
- **Request (`TransformationRequest`)**: Marker type representing the input to a transformation.
- **Container (`TransformationContainer<TEnum>`)**: Holds the working state and data during a transformation, including
  `source` and `target` states.
- **Response**: The final output type returned after successful transformation.

### Behaviors
- **Actions (`OnTransformationAction<TContainer>`)**: Mutate or enrich the container. Can be chained using `andThen`.
- **Guards (`OnTransformationGuard<TContainer>`)**: Validate whether execution should proceed. Return `Mono<Boolean>`.
- **Error Handler (`OnTransformationErrorHandler<TRequest, TResponse>`)**: Maps errors to a fallback `Mono<TResponse>`.

### Providers
- **Container Provider (`TransformationContainerProvider<TRequest, TContainer, TEnum>`)**: Builds the container from the incoming request and desired states.
- **Response Provider (`TransformationResponseProvider<TRequest, TContainer, TResponse>`)**: Maps the final container to your response type.

### Configuration Phases
- **Before Transformation**: Validation guards and pre-processing actions
- **During Transformation**: Core domain logic and state changes
- **After Transformation**: Post-processing, notifications, and after-commit hooks

### Factories (Spring Integration)
- **Action Factory (`OnTransformationActionFactory`)**: Composes multiple actions from Spring beans
- **Guard Factory (`OnTransformationGuardFactory`)**: Composes multiple guards from Spring beans
- **Error Handler Factory (`OnTransformationErrorHandlerFactory`)**: Composes multiple error handlers from Spring beans

---

## Quickstart

### 1. Define Your Domain Types

```kotlin
// States
enum class OrderStatus { 
  Created, Processing, Shipped, Delivered, Cancelled 
}

// Request
sealed class OrderRequest : TransformationRequest {
  data class Create(val customerId: String, val items: List<Item>) : OrderRequest()
  data class Cancel(val orderId: String, val reason: String) : OrderRequest()
}

// Container
data class OrderContainer(
  val order: Order,
  val customer: Customer,
  override val source: OrderStatus? = null,
  override val target: OrderStatus? = null,
) : TransformationContainer<OrderStatus>
```

### 2. Implement Actions and Guards

```kotlin
@Component
class ValidatePaymentAction : OnTransformationAction<OrderContainer> {
  override fun execute(container: OrderContainer): Mono<OrderContainer> =
    Mono.fromCallable {
      // Validate payment logic
      container.copy(order = container.order.copy(paymentValidated = true))
    }
}

@Component
class InventoryCheckGuard : OnTransformationGuard<OrderContainer> {
  // Preferred: New-style API with GuardDecision (thread-safe)
  // Requires: import io.github.mahdibohloul.statemachine.guards.GuardDecision
  //          import io.github.mahdibohloul.statemachine.StateMachineErrorCodeString
  override fun executeDecision(container: OrderContainer): Mono<GuardDecision> =
    Mono.fromCallable {
      val allItemsAvailable = container.order.items.all { item -> checkInventory(item) }
      if (allItemsAvailable) {
        GuardDecision.Allow
      } else {
        GuardDecision.Deny(
          errorCode = StateMachineErrorCodeString.GuardValidationFailed,
          cause = InsufficientInventoryException(container.order.items)
        )
      }
    }
  
  // Legacy: Deprecated boolean-based API (still supported for backward compatibility)
  @Deprecated("Use executeDecision instead", ReplaceWith("executeDecision(container)"))
  override fun execute(container: OrderContainer): Mono<Boolean> =
    executeDecision(container).map { it.isAllowed() }
}
```

### 3. Create Container and Response Providers

```kotlin
@Component
class OrderContainerProvider : TransformationContainerProvider<OrderRequest, OrderContainer, OrderStatus> {
  override fun provideContainer(
    request: OrderRequest,
    source: OrderStatus?,
    target: OrderStatus?
  ): Mono<OrderContainer> = when (request) {
    is OrderRequest.Create -> Mono.fromCallable {
      OrderContainer(
        order = Order.fromRequest(request),
        customer = getCustomer(request.customerId),
        source = source,
        target = target
      )
    }
    is OrderRequest.Cancel -> Mono.fromCallable {
      OrderContainer(
        order = getOrder(request.orderId),
        customer = getCustomerByOrder(request.orderId),
        source = source,
        target = target
      )
    }
  }
}

@Component
class OrderResponseProvider : TransformationResponseProvider<OrderRequest, OrderContainer, Order> {
  override fun provideResponse(request: OrderRequest, container: OrderContainer): Mono<Order> =
    Mono.just(container.order)
}
```

### 4. Implement the Transformer

```kotlin
@StateMachineState
class OrderProcessingTransformer(
  private val containerProvider: OrderContainerProvider,
  private val responseProvider: OrderResponseProvider,
  private val actionFactory: OnTransformationActionFactory,
  private val guardFactory: OnTransformationGuardFactory,
) : StateTransformerAdapter<OrderRequest.Create, OrderContainer, Order, OrderStatus>() {
  
  override fun getState(): OrderStatus = OrderStatus.Processing
  override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

  override fun configure(configurer: StateMachineConfigurer<OrderRequest.Create, OrderContainer, Order, OrderStatus>) {
    configurer.apply {
      sourceState = OrderStatus.Created
      targetState = OrderStatus.Processing
      transformationContainerProvider = containerProvider
      transformationResponseProvider = responseProvider
      onTransformationErrorHandler = OrderErrorHandler()
    }
  }

  override fun configure(configurer: BeforeTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      beforeTransformationGuard = guardFactory.getGuard(
        InventoryCheckGuard::class,
        PaymentValidationGuard::class
      )
      beforeTransformationAction = actionFactory.getAction(
        ValidatePaymentAction::class,
        ReserveInventoryAction::class
      )
    }
  }

  override fun configure(configurer: DuringTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      duringTransformationAction = actionFactory.getAction(
        ProcessPaymentAction::class,
        SaveOrderAction::class
      )
    }
  }

  override fun configure(configurer: AfterTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      afterTransformationAction = actionFactory.getAction(
        SendConfirmationAction::class
      )
      afterCommitTransactionAction = actionFactory.getAction(
        NotifyWarehouseAction::class
      )
    }
  }
}
```

### 5. Execute the Transformation

```kotlin
@RestController
class OrderController(
  private val orderProcessingTransformer: OrderProcessingTransformer
) {
  
  @PostMapping("/orders")
  fun createOrder(@RequestBody request: CreateOrderRequest): Mono<Order> =
    orderProcessingTransformer.transform(
      OrderRequest.Create(request.customerId, request.items)
    )
}
```

---

## Advanced Usage

### Composing Behaviors

Use `andThen` to chain actions and error handlers:

```kotlin
// Chain multiple actions
val compositeAction = ValidatePaymentAction() 
  .andThen(ReserveInventoryAction())
  .andThen(SendNotificationAction())

// Chain multiple error handlers
val compositeErrorHandler = LogErrorHandler()
  .andThen(FallbackResponseHandler())
  .andThen(RetryHandler())
```

### Transformer Ordering

Control execution order using `getOrder()`:

```kotlin
object TransformerOrder {
  const val HIGH_PRIORITY = Ordered.HIGHEST_PRECEDENCE
  const val MEDIUM_PRIORITY = Ordered.HIGHEST_PRECEDENCE + 10
  const val LOW_PRIORITY = Ordered.LOWEST_PRECEDENCE
}

class HighPriorityTransformer : StateTransformerAdapter<...>() {
  override fun getOrder(): Int = TransformerOrder.HIGH_PRIORITY
}
```

### Custom Validation in canHandle()

```kotlin
abstract class AbstractOrderTransformer<TRequest : OrderRequest> : 
  StateTransformerAdapter<TRequest, OrderContainer, Order, OrderStatus>() {
  
  override fun canHandle(transformerIdentifier: StateMachineStateFactory.TransformerIdentifier) {
    super.canHandle(transformerIdentifier)
    val request = transformerIdentifier.getTransformationRequest<OrderRequest>()
    
    require(isSupportedOrderType(request)) {
      "Order type ${request.type} is not supported by this transformer"
    }
  }
  
  protected abstract fun isSupportedOrderType(request: OrderRequest): Boolean
}
```

### Domain-Specific Error Handling

```kotlin
@Component
class OrderErrorHandler : OnTransformationErrorHandler<OrderRequest, Order> {
  override fun onError(request: OrderRequest, error: Throwable): Mono<Order> =
    when (error) {
      is InsufficientInventoryException -> 
        Mono.error(OrderException.InventoryUnavailable(error.itemIds))
      is PaymentFailedException -> 
        Mono.error(OrderException.PaymentDeclined(error.reason))
      is DuplicateKeyException -> 
        Mono.error(OrderException.DuplicateOrder(request.orderId))
      else -> Mono.error(error)
    }
}
```

### Guard Decision API and Thread Safety

The library provides a thread-safe guard validation API through the `GuardDecision` sealed interface. This prevents concurrency issues when multiple threads access stateless guards.

**Why GuardDecision?**

In concurrent environments, stateless guards that rely on instance methods to provide error codes can experience race conditions. The `GuardDecision` API captures error codes and causes in immutable data structures at decision time, ensuring thread-safe error handling.

**Using GuardDecision:**

```kotlin
@Component
class PaymentValidationGuard : OnTransformationGuard<OrderContainer> {
  override fun executeDecision(container: OrderContainer): Mono<GuardDecision> =
    Mono.fromCallable {
      val isValid = validatePayment(container.order.paymentMethod)
      if (isValid) {
        GuardDecision.Allow
      } else {
        GuardDecision.Deny(
          errorCode = CustomErrorCode.PaymentValidationFailed,
          cause = PaymentValidationException("Invalid payment method")
        )
      }
    }
}
```

**Legacy Boolean API (Deprecated):**

The legacy `execute()` method returning `Mono<Boolean>` is still supported for backward compatibility but is deprecated. The default implementation of `executeDecision()` adapts legacy guards automatically.

```kotlin
// Legacy approach (deprecated but still works)
@Component
class LegacyGuard : OnTransformationGuard<OrderContainer> {
  @Deprecated("Use executeDecision instead")
  override fun execute(container: OrderContainer): Mono<Boolean> =
    Mono.just(validate(container))
}
```

**Benefits:**
- **Thread-safe**: Error codes are captured in immutable `GuardDecision.Deny` objects
- **Type-safe**: Sealed interface ensures exhaustive handling
- **Backward compatible**: Legacy boolean-based guards continue to work
- **Rich error information**: Error codes and causes are explicitly captured

---

## Spring Integration

### Automatic Bean Discovery

Annotate state-specific components with `@StateMachineState`:

```kotlin
@StateMachineState("Processing")
class OrderProcessingTransformer(
  // dependencies injected by Spring
) : StateTransformerAdapter<...>()

@StateMachineState("Approved") 
class OrderApprovedTransformer(
  // dependencies injected by Spring
) : StateTransformerAdapter<...>()
```

### Factory-Based Composition

Use factories to compose multiple behaviors from Spring beans:

```kotlin
@Configuration
class StateMachineConfiguration {
  
  @Bean
  fun actionFactory(applicationContext: ApplicationContext) = 
    OnTransformationActionFactory(applicationContext)
    
  @Bean  
  fun guardFactory(applicationContext: ApplicationContext) = 
    OnTransformationGuardFactory(applicationContext)
}

// In your transformer
override fun configure(configurer: BeforeTransformationConfigurer<OrderContainer, OrderStatus>) {
  configurer.apply {
    // Compose multiple guards from Spring beans
    beforeTransformationGuard = guardFactory.getGuard(
      PaymentValidationGuard::class,
      InventoryCheckGuard::class,
      CustomerEligibilityGuard::class
    )
    
    // Compose multiple actions from Spring beans  
    beforeTransformationAction = actionFactory.getAction(
      ValidatePaymentAction::class,
      ReserveInventoryAction::class,
      SendNotificationAction::class
    )
  }
}
```

### Manual Wiring (Non-Spring)

You can use the library without Spring by manually wiring dependencies:

```kotlin
class ManualOrderTransformer : StateTransformerAdapter<OrderRequest, OrderContainer, Order, OrderStatus>() {
  
  override fun configure(configurer: BeforeTransformationConfigurer<OrderContainer, OrderStatus>) {
    configurer.apply {
      // Manually compose behaviors
      beforeTransformationGuard = PaymentValidationGuard()
        .andThen(InventoryCheckGuard())
        .andThen(CustomerEligibilityGuard())
        
      beforeTransformationAction = ValidatePaymentAction()
        .andThen(ReserveInventoryAction())
        .andThen(SendNotificationAction())
    }
  }
}
```

---

## Transactions and After-Commit Hooks

The library provides transaction-aware execution for after-commit actions. When a reactive transaction is active, 
`afterCommitTransactionAction` will only execute after the transaction successfully commits.

### Basic Usage

```kotlin
override fun configure(configurer: AfterTransformationConfigurer<OrderContainer, OrderStatus>) {
  configurer.apply {
    // This runs immediately after the main transformation
    afterTransformationAction = actionFactory.getAction(
      SendConfirmationAction::class,
      UpdateInventoryAction::class
    )
    
    // This runs only after transaction commit (if transaction is active)
    afterCommitTransactionAction = actionFactory.getAction(
      NotifyWarehouseAction::class,
      SendExternalNotificationAction::class
    )
  }
}
```

### Transaction Scenarios

```kotlin
// Scenario 1: No active transaction
// - afterTransformationAction executes
// - afterCommitTransactionAction is skipped

// Scenario 2: Active transaction that commits successfully  
// - afterTransformationAction executes
// - Transaction commits
// - afterCommitTransactionAction executes

// Scenario 3: Active transaction that rolls back
// - afterTransformationAction executes  
// - Transaction rolls back
// - afterCommitTransactionAction is skipped
```

### Use Cases for After-Commit Actions

- **External API calls**: Only notify external systems after data is persisted
- **Event publishing**: Publish domain events only after successful persistence
- **Audit logging**: Log state changes only after they're committed
- **Cache invalidation**: Update caches only after database changes are committed

### Example: Order Processing with Notifications

```kotlin
override fun configure(configurer: AfterTransformationConfigurer<OrderContainer, OrderStatus>) {
  configurer.apply {
    // Immediate actions (run before commit)
    afterTransformationAction = actionFactory.getAction(
      UpdateOrderStatusAction::class,
      SendCustomerEmailAction::class
    )
    
    // After-commit actions (run only after successful commit)
    afterCommitTransactionAction = actionFactory.getAction(
      NotifyWarehouseAction::class,        // External system notification
      PublishOrderCreatedEvent::class,     // Event publishing
      UpdateAnalyticsAction::class,        // Analytics tracking
      InvalidateCacheAction::class         // Cache invalidation
    )
  }
}
```

---

## Error Handling

### Global Error Handler

Set a global error handler via `StateMachineConfigurer.onTransformationErrorHandler`:

```kotlin
override fun configure(configurer: StateMachineConfigurer<OrderRequest, OrderContainer, Order, OrderStatus>) {
  configurer.apply {
    onTransformationErrorHandler = object : OnTransformationErrorHandler<OrderRequest, Order> {
      override fun onError(request: OrderRequest, error: Throwable): Mono<Order> =
        when (error) {
          is InsufficientInventoryException -> 
            Mono.error(OrderException.InventoryUnavailable(error.itemIds))
          is PaymentFailedException -> 
            Mono.error(OrderException.PaymentDeclined(error.reason))
          is DuplicateKeyException -> 
            Mono.error(OrderException.DuplicateOrder(request.orderId))
          else -> Mono.error(error)
        }
    }
  }
}
```

### Composed Error Handlers

Use factories to compose multiple error handlers:

```kotlin
override fun configure(configurer: StateMachineConfigurer<OrderRequest, OrderContainer, Order, OrderStatus>) {
  configurer.apply {
    onTransformationErrorHandler = errorHandlerFactory.getErrorHandler(
      LogErrorHandler::class,
      FallbackResponseHandler::class,
      RetryHandler::class
    )
  }
}
```

### Common Exceptions

- `StateMachineException.SourceAndTargetAreEqualException`: Source and target states are identical
- `StateMachineException.NoContainerProviderConfiguredException`: No container provider configured

### Error Handler Examples

```kotlin
@Component
class LogErrorHandler : OnTransformationErrorHandler<OrderRequest, Order> {
  override fun onError(request: OrderRequest, error: Throwable): Mono<Order> =
    Mono.fromRunnable {
      logger.error("Order transformation failed for request: $request", error)
    }.then(Mono.error(error))
}

@Component  
class FallbackResponseHandler : OnTransformationErrorHandler<OrderRequest, Order> {
  override fun onError(request: OrderRequest, error: Throwable): Mono<Order> =
    Mono.just(Order.failed(error.message ?: "Unknown error"))
}

@Component
class RetryHandler : OnTransformationErrorHandler<OrderRequest, Order> {
  override fun onError(request: OrderRequest, error: Throwable): Mono<Order> =
    if (isRetryableError(error)) {
      Mono.error(error) // Let retry mechanism handle it
    } else {
      Mono.error(error)
    }
}
```

---

## Testing

### Unit Testing Actions and Guards

```kotlin
class OrderActionTest {
  
  @Test
  fun `should validate payment successfully`() {
    // Given
    val container = OrderContainer(
      order = Order(paymentValidated = false),
      customer = Customer(id = "123")
    )
    val action = ValidatePaymentAction()
    
    // When & Then
    action.execute(container)
      .`as`(StepVerifier::create)
      .expectNextMatches { it.order.paymentValidated }
      .verifyComplete()
  }
  
  @Test
  fun `should fail when payment is invalid`() {
    // Given
    val container = OrderContainer(
      order = Order(paymentValidated = false, paymentMethod = "INVALID"),
      customer = Customer(id = "123")
    )
    val action = ValidatePaymentAction()
    
    // When & Then
    action.execute(container)
      .`as`(StepVerifier::create)
      .verifyError(PaymentValidationException::class.java)
  }
}

@Test
fun `should validate guard successfully with GuardDecision`() {
  // Given
  val container = OrderContainer(
    order = Order(items = listOf(Item(id = "item1", quantity = 2))),
    customer = Customer(id = "123")
  )
  val guard = InventoryCheckGuard()
  
  // When & Then
  guard.executeDecision(container)
    .`as`(StepVerifier::create)
    .expectNextMatches { it is GuardDecision.Allow }
    .verifyComplete()
}

@Test
fun `should deny guard validation with error code`() {
  // Given
  val container = OrderContainer(
    order = Order(items = listOf(Item(id = "out-of-stock", quantity = 100))),
    customer = Customer(id = "123")
  )
  val guard = InventoryCheckGuard()
  
  // When & Then
  guard.executeDecision(container)
    .`as`(StepVerifier::create)
    .expectNextMatches { decision ->
      decision is GuardDecision.Deny &&
      decision.errorCode == StateMachineErrorCodeString.GuardValidationFailed
    }
    .verifyComplete()
}
```

### Testing Composed Behaviors

```kotlin
@Test
fun `should execute multiple actions in sequence`() {
  // Given
  val container = OrderContainer(order = Order(value = 100))
  val action1 = MultiplyByTwoAction()
  val action2 = AddTenAction()
  val compositeAction = action1.andThen(action2)
  
  // When & Then
  compositeAction.execute(container)
    .`as`(StepVerifier::create)
    .expectNextMatches { it.order.value == 210 } // (100 * 2) + 10
    .verifyComplete()
}
```

### Integration Testing Transformers

```kotlin
@SpringBootTest
class OrderTransformerIntegrationTest {
  
  @Autowired
  private lateinit var orderProcessingTransformer: OrderProcessingTransformer
  
  @Test
  fun `should process order successfully`() {
    // Given
    val request = OrderRequest.Create(
      customerId = "123",
      items = listOf(Item(id = "item1", quantity = 2))
    )
    
    // When & Then
    orderProcessingTransformer.transform(request)
      .`as`(StepVerifier::create)
      .expectNextMatches { order -> 
        order.status == OrderStatus.Processing &&
        order.paymentValidated &&
        order.inventoryReserved
      }
      .verifyComplete()
  }
  
  @Test
  fun `should handle insufficient inventory`() {
    // Given
    val request = OrderRequest.Create(
      customerId = "123", 
      items = listOf(Item(id = "out-of-stock", quantity = 100))
    )
    
    // When & Then
    orderProcessingTransformer.transform(request)
      .`as`(StepVerifier::create)
      .verifyError(OrderException.InventoryUnavailable::class.java)
  }
}
```

### Testing Error Handlers

```kotlin
@Test
fun `should handle payment failure gracefully`() {
  // Given
  val request = OrderRequest.Create(customerId = "123", items = emptyList())
  val error = PaymentFailedException("Card declined")
  val errorHandler = OrderErrorHandler()
  
  // When & Then
  errorHandler.onError(request, error)
    .`as`(StepVerifier::create)
    .verifyError(OrderException.PaymentDeclined::class.java)
}
```

### Mocking Dependencies

```kotlin
@ExtendWith(MockitoExtension::class)
class OrderTransformerTest {
  
  @Mock
  private lateinit var orderService: OrderService
  
  @Mock
  private lateinit var paymentService: PaymentService
  
  @Test
  fun `should save order after processing`() {
    // Given
    val container = OrderContainer(order = Order(id = "123"))
    val action = SaveOrderAction(orderService)
    
    whenever(orderService.save(any())).thenReturn(Mono.just(Order(id = "123")))
    
    // When
    action.execute(container)
      .`as`(StepVerifier::create)
      .expectNext(container)
      .verifyComplete()
    
    // Then
    verify(orderService).save(container.order)
  }
}
```

---

## Best Practices

### 1. State Design
- Use enums for states to ensure type safety
- Keep state names descriptive and domain-specific
- Avoid too many states - consider if some can be combined

### 2. Container Design
- Keep containers immutable when possible
- Include all necessary domain data in the container
- Use sealed classes for different request types

### 3. Action Composition
- Keep actions focused on single responsibilities
- Use `andThen` to compose related actions
- Prefer small, testable actions over large monolithic ones

### 4. Guard Design
- Guards should be pure validation logic
- Prefer `executeDecision()` returning `GuardDecision` over legacy `execute()` returning `Boolean`
- Use `GuardDecision.Deny` with specific error codes and causes for failed validations
- Return meaningful error codes and exception causes for better error handling
- Consider using domain-specific exception types in `GuardDecision.Deny`

### 5. Error Handling
- Use domain-specific exceptions
- Provide fallback responses when appropriate
- Log errors with sufficient context

### 6. Transaction Management
- Use after-commit actions for external system notifications
- Keep transaction boundaries clear and minimal
- Test both transactional and non-transactional scenarios

### 7. Testing Strategy
- Test each action and guard in isolation
- Use integration tests for full transformation flows
- Mock external dependencies appropriately

### 8. Performance Considerations
- Use reactive patterns throughout
- Avoid blocking operations in actions and guards
- Consider caching for frequently accessed data

---

## Real-World Example: Delivery Request State Machine

This library is used in production at Tapsi for managing delivery request state transitions. The delivery domain includes:

- **States**: `Created`, `Processing`, `Assigned`, `PickedUp`, `Delivered`, `Cancelled`
- **Request Types**: Creation, modification, cancellation
- **Complex Validation**: Payment validation, inventory checks, driver availability
- **External Integrations**: Payment gateways, driver apps, customer notifications
- **Transaction Management**: Database persistence with after-commit notifications

Key patterns from the production implementation:
- Abstract base transformers for common functionality
- Factory-based composition of behaviors
- Comprehensive error handling with domain-specific exceptions
- Transaction-aware notifications to external systems
- Extensive logging and monitoring

---

## Version Matrix

- Kotlin: 1.9.x
- Reactor: 3.7.x
- Spring: 6.1+/Boot 3.3+
- Java: 21

---

## License

MIT License. See `LICENSE` for details.


