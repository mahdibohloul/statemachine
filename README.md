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

### At a glance

```kotlin
class MyTransformer : StateTransformerAdapter<MyRequest, MyContainer, MyResponse, MyState>() {
  override fun getState(): MyState = MyState.Created

  override fun configure(configurer: StateMachineConfigurer<MyRequest, MyContainer, MyResponse, MyState>) {
    configurer.sourceState = MyState.Created
    configurer.targetState = MyState.Approved
    configurer.transformationContainerProvider = MyContainerProvider()
    configurer.transformationResponseProvider = MyResponseProvider()
    configurer.onTransformationErrorHandler = MyErrorHandler()
  }

  override fun configure(configurer: BeforeTransformationConfigurer<MyContainer, MyState>) {
    configurer.beforeTransformationGuard = EnsureCanApproveGuard()
    configurer.beforeTransformationAction = EnrichContextAction()
  }

  override fun configure(configurer: DuringTransformationConfigurer<MyContainer, MyState>) {
    configurer.duringTransformationAction = ApproveDomainAction()
  }

  override fun configure(configurer: AfterTransformationConfigurer<MyContainer, MyState>) {
    configurer.afterTransformationGuard = PersistedGuard()
    configurer.afterTransformationAction = PublishDomainEventsAction()
    configurer.afterCommitTransactionAction = NotifyUsersAfterCommitAction()
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
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Minimums:

- Kotlin 1.9+
- Java 21+
- Spring 6.1+/Boot 3.3+ (optional, for Spring integration)
- Reactor 3.7+

---

## Core Concepts

- **States (`TEnum : Enum<*>`)**: Your domain states (e.g., `Created`, `Approved`).
- **Request (`TransformationRequest`)**: Marker type representing the input to a transformation.
- **Container (`TransformationContainer<TEnum>`)**: Holds the working state and data during a transformation, including
  `source` and `target` states.
- **Actions (`OnTransformationAction<TContainer>`)**: Mutate or enrich the container.
- **Guards (`OnTransformationGuard<TContainer>`)**: Validate whether execution should proceed.
- **Error Handler (`OnTransformationErrorHandler<TRequest, TResponse>`)**: Maps errors to a fallback `Mono<TResponse>`.
- **Providers**:
    - `TransformationContainerProvider<TRequest, TContainer, TEnum>`: Builds the container from the incoming request and
      desired states.
    - `TransformationResponseProvider<TRequest, TContainer, TResponse>`: Maps the final container to your response type.
- **Configurators**:
    - `BeforeTransformationConfigurer` — validation and pre-processing
    - `DuringTransformationConfigurer` — core domain action(s)
    - `AfterTransformationConfigurer` — post-processing and after-commit hooks
- **StateMachineConfigurer**: Sets error handler, container/response providers, and source/target states.

---

## Quickstart

Define your states, request, and container:

```kotlin
enum class MyState { Created, Approved }

class MyRequest(/* fields */) : TransformationRequest

data class MyContainer(
  val domain: DomainModel,
  override val source: MyState? = null,
  override val target: MyState? = null,
) : TransformationContainer<MyState>
```

Implement a transformer with phased configuration:

```kotlin
class ApproveTransformer : StateTransformerAdapter<MyRequest, MyContainer, MyResponse, MyState>() {
  override fun getState(): MyState = MyState.Created

  override fun configure(c: StateMachineConfigurer<MyRequest, MyContainer, MyResponse, MyState>) {
    c.sourceState = MyState.Created
    c.targetState = MyState.Approved
    c.transformationContainerProvider = MyContainerProvider()
    c.transformationResponseProvider = MyResponseProvider()
  }

  override fun configure(c: BeforeTransformationConfigurer<MyContainer, MyState>) {
    c.beforeTransformationGuard = EnsureCanApproveGuard()
    c.beforeTransformationAction = EnrichContextAction()
  }

  override fun configure(c: DuringTransformationConfigurer<MyContainer, MyState>) {
    c.duringTransformationAction = ApproveDomainAction()
  }

  override fun configure(c: AfterTransformationConfigurer<MyContainer, MyState>) {
    c.afterTransformationGuard = PersistedGuard()
    c.afterTransformationAction = PublishDomainEventsAction()
    c.afterCommitTransactionAction = NotifyUsersAfterCommitAction()
  }
}
```

Execute the transformation:

```kotlin
ApproveTransformer()
  .transform(MyRequest(/* ... */))
  .subscribe { response -> /* handle */ }
```

---

## Composing behaviors

Use `andThen` to chain actions and error handlers from `support/OperatorExtensions.kt` and
`support/CompositeBehaviors.kt`:

```kotlin
val action = FirstAction() andThen SecondAction() andThen ThirdAction()
val errorHandler = FallbackHandler() andThen LogAndRethrowHandler()
```

---

## Spring integration

- Annotate state-specific components with `@StateMachineState` so factories can discover them from the
  `ApplicationContext`.
  ```kotlin
  @StateMachineState("Approved")
  class ApprovedStateBean
  ```
- Factories like `OnTransformationActionFactory`, `OnTransformationGuardFactory`, and others can assemble behaviors at
  runtime based on Spring beans.

Note: Spring is optional; you can wire everything manually without Spring.

---

## Transactions and after-commit hooks

`AfterTransformationConfigurer` registers a transaction synchronization via `StateTransformerTransactionSynchronization`
when there's an active reactive transaction. The `afterCommitTransactionAction` runs only after a successful commit.

```kotlin
override fun configure(c: AfterTransformationConfigurer<MyContainer, MyState>) {
  c.afterCommitTransactionAction = NotifyUsersAfterCommitAction()
}
```

If there is no active transaction, this action is skipped.

---

## Error handling

Set a global error handler via `StateMachineConfigurer.onTransformationErrorHandler` to recover with a fallback response
or rethrow.

```kotlin
configurer.onTransformationErrorHandler = object : OnTransformationErrorHandler<MyRequest, MyResponse> {
  override fun onError(request: MyRequest, error: Throwable): Mono<MyResponse> =
    Mono.just(MyResponse.failed(error.message))
}
```

Common exceptions:

- `StateMachineException.SourceAndTargetAreEqualException`
- `StateMachineException.NoContainerProviderConfiguredException`

---

## Testing

Use `reactor-test` `StepVerifier` to assert flows. Example from the test suite composes two actions and verifies the
result.

```kotlin
CompositeBehaviors.CompositeOnTransformationAction(mutableListOf(ActionA(), ActionB()))
  .execute(container)
  .`as`(StepVerifier::create)
  .expectNextMatches { it.value == expected }
  .verifyComplete()
```

---

## Version Matrix

- Kotlin: 1.9.x
- Reactor: 3.7.x
- Spring: 6.1+/Boot 3.3+
- Java: 21

---

## License

MIT License. See `LICENSE` for details.


