package io.github.mahdibohloul.statemachine.factories

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.guards.OnTransformationGuard
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class OnTransformationGuardFactory(
  private val applicationContext: ApplicationContext,
) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  private val guardMap: Map<KClass<out OnTransformationGuard<*>>, OnTransformationGuard<*>> by lazy {
    val beans = applicationContext.getBeansOfType(OnTransformationGuard::class.java).values
    val keys =
      beans.map { it::class.getOriginalClass() }.filterIsInstance<KClass<out OnTransformationGuard<*>>>()

    require(keys.size == beans.size) {
      "There is a mismatch within the transformation guard beans and transformation guard classes"
    }
    logger.info("Discovered ${beans.size} transformation guards")
    return@lazy keys.zip(beans).toMap()
  }

  fun <T : TransformationContainer<*>> getGuard(
    vararg guardClass: KClass<out OnTransformationGuard<*>>,
  ): OnTransformationGuard<T> = guardClass.mapNotNull { guardMap[it] }
    .filterIsInstance<OnTransformationGuard<T>>()
    .let { guards ->
      if (guards.isEmpty()) {
        logger.warn("No transformation guard found for classes: ${guardClass.joinToString()}")
      }
      return@let CompositeBehaviors.CompositeOnTransformationGuard(guards.toMutableList())
    }
}
