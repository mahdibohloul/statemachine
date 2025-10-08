package io.github.mahdibohloul.statemachine.factories

import box.tapsi.libs.utilities.getOriginalClass
import io.github.mahdibohloul.statemachine.TransformationRequest
import io.github.mahdibohloul.statemachine.handlers.OnTransformationErrorHandler
import io.github.mahdibohloul.statemachine.support.CompositeBehaviors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

class OnTransformationErrorHandlerFactory(
  private val applicationContext: ApplicationContext,
) {
  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

  private val errorHandlerMap: Map<
    KClass<out OnTransformationErrorHandler<*, *>>,
    OnTransformationErrorHandler<*, *>,
    > by lazy {
    val beans = applicationContext.getBeansOfType(OnTransformationErrorHandler::class.java).values
    val keys =
      beans.map { it::class.getOriginalClass() }.filterIsInstance<KClass<out OnTransformationErrorHandler<*, *>>>()

    require(keys.size == beans.size) {
      "There is a mismatch within the transformation error handler beans and transformation error handler names"
    }

    logger.info("Discovered ${beans.size} transformation error handlers")
    return@lazy keys.zip(beans).toMap()
  }

  fun <TReq : TransformationRequest, TRes : Any> getErrorHandler(
    vararg errorHandlerClass: KClass<out OnTransformationErrorHandler<*, *>>,
  ): OnTransformationErrorHandler<TReq, TRes> = errorHandlerClass.mapNotNull { errorHandlerMap[it] }
    .filterIsInstance<OnTransformationErrorHandler<TReq, TRes>>()
    .let { handlers ->
      if (handlers.isEmpty()) {
        logger.warn("No transformation error handler found for classes: ${errorHandlerClass.joinToString()}")
      }
      return@let CompositeBehaviors.CompositeOnTransformationErrorHandler(handlers.toMutableList())
    }
}
