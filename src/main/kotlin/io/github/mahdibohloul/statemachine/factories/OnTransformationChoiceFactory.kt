package io.github.mahdibohloul.statemachine.factories

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.TransformationContainer
import io.github.mahdibohloul.statemachine.choices.OnTransformationChoice
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class OnTransformationChoiceFactory(
  private val applicationContext: ApplicationContext,
) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  private val choiceMap: Map<KClass<out OnTransformationChoice<*>>, OnTransformationChoice<*>> by lazy {
    val beans = applicationContext.getBeansOfType(OnTransformationChoice::class.java).values
    val keys = beans.map { it::class.getOriginalClass() }.filterIsInstance<KClass<out OnTransformationChoice<*>>>()

    require(keys.size == beans.size) {
      "There is a mismatch within the transformation choice beans and transformation choice names"
    }

    logger.info("Discovered ${beans.size} transformation choices")
    return@lazy keys.zip(beans).toMap()
  }

  fun <T : TransformationContainer<*>> getChoice(
    vararg choiceClass: KClass<out OnTransformationChoice<*>>,
  ): OnTransformationChoice<T> = choiceClass.mapNotNull { choiceMap[it] }
    .filterIsInstance<OnTransformationChoice<T>>()
    .let { choices ->
      if (choices.isEmpty()) {
        logger.warn("No transformation choice found for classes: ${choiceClass.joinToString()}")
      }
      return@let CompositeBehaviors.CompositeOnTransformationChoice(choices.toMutableList())
    }
}
