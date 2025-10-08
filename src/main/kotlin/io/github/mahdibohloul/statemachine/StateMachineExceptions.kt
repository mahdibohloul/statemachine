package io.github.mahdibohloul.statemachine

import box.tapsi.libs.utilities.ErrorCodeString
import box.tapsi.libs.utilities.TapsiException

sealed class StateMachineException(
  message: String,
  val validationFailureErrorCodeString: ErrorCodeString,
  val source: Enum<*>?,
  val target: Enum<*>?,
) : TapsiException(message) {
  open class GuardValidationException(
    guardName: String,
    validationFailureErrorCodeString: ErrorCodeString,
    source: Enum<*>?,
    target: Enum<*>?,
    override val cause: Throwable? = null,
  ) : StateMachineException(
    "Guard validation failed for guard $guardName",
    validationFailureErrorCodeString = validationFailureErrorCodeString,
    source = source,
    target = target,
  ) {
    override fun getErrorCodeString(): ErrorCodeString = validationFailureErrorCodeString
  }

  open class NoContainerProviderConfiguredException(
    source: Enum<*>?,
    target: Enum<*>?,
  ) : StateMachineException(
    "No container provider configured for source $source and target $target",
    source = source,
    target = target,
    validationFailureErrorCodeString = StateMachineErrorCodeString.NoContainerProviderConfigured,
  )

  open class SourceAndTargetAreEqualException(
    source: Enum<*>?,
    target: Enum<*>?,
  ) : StateMachineException(
    "Source and target are equal for source $source and target $target",
    source = source,
    target = target,
    validationFailureErrorCodeString = StateMachineErrorCodeString.SourceAndTargetAreEqual,
  )
}

enum class StateMachineErrorCodeString(override val codeString: String) : ErrorCodeString {
  GuardValidationFailed("GuardValidationFailed"),
  NoContainerProviderConfigured("NoContainerProviderConfigured"),
  SourceAndTargetAreEqual("SourceAndTargetAreEqual"),
}
