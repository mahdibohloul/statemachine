package io.github.mahdibohloul.statemachine.factories

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.actions.OnTransformationAction
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class OnTransformationActionFactory(
  private val applicationContext: ApplicationContext,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  private val actionMap: Map<KClass<out OnTransformationAction<*>>, OnTransformationAction<*>> by lazy {
    val beans = applicationContext.getBeansOfType(OnTransformationAction::class.java).values
    val keys =
      beans.map { it::class.getOriginalClass() }.filterIsInstance<KClass<out OnTransformationAction<*>>>()

    require(keys.size == beans.size) {
      "There is a mismatch within the transformation action beans and transformation action classes"
    }
    logger.info("Discovered ${beans.size} transformation actions")
    return@lazy keys.zip(beans).toMap()
  }

  fun <T : TransformationContainer<*>> getAction(
    vararg actionClass: KClass<out OnTransformationAction<*>>,
  ): OnTransformationAction<T> = actionClass.mapNotNull { actionMap[it] }
    .filterIsInstance<OnTransformationAction<T>>()
    .let { actions ->
      if (actions.isEmpty()) {
        logger.warn("No transformation action found for classes: ${actionClass.joinToString()}")
      }
      return@let CompositeBehaviors.CompositeOnTransformationAction(actions.toMutableList())
    }
}
