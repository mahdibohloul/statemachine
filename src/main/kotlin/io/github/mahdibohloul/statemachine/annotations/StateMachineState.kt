package io.github.mahdibohloul.statemachine.annotations

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component

/**
 * This annotation will be used to annotate the state of the state machine
 * The factory will be searching for this annotation in the Spring Context
 * and will create a state machine state for each annotated class.
 *
 * @param value The name of the state.
 * @see io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class StateMachineState(
  @get:AliasFor(annotation = Component::class)
  val value: String = "",
)
