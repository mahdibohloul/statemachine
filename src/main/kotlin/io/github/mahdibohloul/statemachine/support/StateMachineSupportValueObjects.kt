package io.github.mahdibohloul.statemachine.support

/**
 * Represents the result of a state machine component's capability check.
 * This sealed class is used to determine whether a state transformer, guard, action, or other
 * component can handle a specific transformation scenario.
 *
 * The result is used internally by the state machine framework to:
 * - Filter applicable transformers for a given transformation request
 * - Provide detailed error information when no suitable transformer is found
 * - Enable proper error handling and debugging in the transformation selection process
 *
 * @see checkAsResult
 * @see io.github.mahdibohloul.statemachine.factories.StateMachineStateFactory.getSortedSupportedTransformers
 */
sealed class StateMachineSupportResult {

  /**
   * Indicates that a state machine component can successfully handle the transformation scenario.
   * This is a singleton object representing successful capability validation.
   */
  data object Success : StateMachineSupportResult()

  /**
   * Indicates that a state machine component cannot handle the transformation scenario.
   * Contains the specific error that prevented the component from being applicable.
   *
   * @param error The exception that explains why the component cannot handle the scenario
   */
  class Failure(val error: Throwable) : StateMachineSupportResult()

  companion object {
    /**
     * Creates a success result indicating that a component can handle the transformation scenario.
     *
     * @return A [Success] instance
     */
    fun success(): StateMachineSupportResult = Success

    /**
     * Creates a failure result with the specified error.
     *
     * @param error The exception that explains why the component cannot handle the scenario
     * @return A [Failure] instance containing the error
     */
    fun failure(error: Throwable): StateMachineSupportResult = Failure(error)
  }
}
