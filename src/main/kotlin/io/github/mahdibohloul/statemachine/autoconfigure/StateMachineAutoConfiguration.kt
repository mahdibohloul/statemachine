package io.github.mahdibohloul.statemachine.autoconfigure

import io.github.mahdibohloul.statemachine.factories.OnTransformationActionFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationChoiceFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationErrorHandlerFactory
import io.github.mahdibohloul.statemachine.factories.OnTransformationGuardFactory
import io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
  OnTransformationActionFactory::class,
  OnTransformationChoiceFactory::class,
  OnTransformationErrorHandlerFactory::class,
  OnTransformationGuardFactory::class,
  StateMachineStateFactory::class,
)
class StateMachineAutoConfiguration
