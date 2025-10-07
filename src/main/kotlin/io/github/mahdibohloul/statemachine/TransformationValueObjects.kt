package io.github.mahdibohloul.statemachine

/**
 * This interface is used to define the type of the request that should be transformed.
 */
interface TransformationRequest

/**
 * This interface is used to define the container of a transformation,
 * The container will be propagated through the chain of the transformation.
 *
 * @param TEnum The type of the enum that will be used as the source and target of the transformation
 */
interface TransformationContainer<TEnum : Enum<*>> {
  val source: TEnum?
  val target: TEnum?
}
