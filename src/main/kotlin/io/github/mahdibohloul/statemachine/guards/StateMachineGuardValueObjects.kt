package io.github.mahdibohloul.statemachine.guards

import box.tapsi.libs.utilities.ErrorCodeString
import io.github.mahdibohloul.statemachine.StateMachineErrorCodeString

sealed interface GuardDecision {
  data object Allow : GuardDecision
  data class Deny(
    val errorCode: ErrorCodeString = StateMachineErrorCodeString.GuardValidationFailed,
    val cause: Throwable? = null,
  ) : GuardDecision
}

fun GuardDecision.isAllowed(): Boolean = this is GuardDecision.Allow
fun GuardDecision.isDenied(): Boolean = this is GuardDecision.Deny
