package io.github.mahdibohloul.statemachine.support.transaction

import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import org.springframework.transaction.reactive.TransactionSynchronization
import reactor.core.publisher.Mono

/**
 * A transaction synchronization implementation that executes an action after a transaction is committed.
 * This class is used to perform post-commit operations on a transformation container.
 *
 * @param TContainer The type of transformation container that holds the state and data for transformation
 * @property afterCommitAction The action to be executed after the transaction is committed
 * @property container The transformation container instance that will be passed to the after-commit action
 */
class StateTransformerTransactionSynchronization<TContainer : TransformationContainer<*>>(
  private val afterCommitAction: OnTransformationAction<TContainer>,
  private val container: TContainer,
) : TransactionSynchronization {
  /**
   * Executes the after-commit action on the transformation container.
   * This method is called by Spring's transaction management after a successful transaction commit.
   *
   * @return A Mono<Void> that completes when the after-commit action is finished
   */
  override fun afterCommit(): Mono<Void> = afterCommitAction.execute(container).then()
}
