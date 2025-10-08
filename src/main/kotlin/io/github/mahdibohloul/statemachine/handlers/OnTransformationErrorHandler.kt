package io.github.mahdibohloul.statemachine.handlers

import io.github.mahdibohloul.statemachine.TransformationRequest
import reactor.core.publisher.Mono

/**
 * Interface representing a handler for managing errors that occur during the transformation process.
 *
 * This handler allows implementing logic to process and potentially recover from errors
 * that occur when handling a `TransformationRequest`.
 *
 * @param TRequest the type of the request that implements the `TransformationRequest` interface.
 * @param TResponse the type of the response being handled, represented as any type.
 */
interface OnTransformationErrorHandler<TRequest : TransformationRequest, TResponse : Any> {
  /**
   * Handles an error that occurs during a transformation process.
   *
   * @param request The transformation request that was being processed when the error occurred.
   * @param error The exception or error that occurred.
   * @return A [Mono] emitting a response of type [TResponse] upon error handling or propagating the error.
   */
  fun onError(request: TRequest, error: Throwable): Mono<TResponse>
}
