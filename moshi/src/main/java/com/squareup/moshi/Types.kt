/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.squareup.moshi

import com.squareup.moshi.internal.EMPTY_TYPE_ARRAY
import com.squareup.moshi.internal.GenericArrayTypeImpl
import com.squareup.moshi.internal.ParameterizedTypeImpl
import com.squareup.moshi.internal.WildcardTypeImpl
import com.squareup.moshi.internal.getGenericSupertype
import com.squareup.moshi.internal.resolve
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Collections
import java.util.Properties
import javax.annotation.CheckReturnValue
import java.lang.annotation.Annotation as JavaAnnotation

/** Factory methods for types. */
@CheckReturnValue
public object Types {

  @JvmStatic
  public fun generatedJsonAdapterName(clazz: Class<*>): String {
    if (clazz.getAnnotation(JsonClass::class.java) == null) {
      throw IllegalArgumentException("Class does not have a JsonClass annotation: $clazz")
    }
    return generatedJsonAdapterName(clazz.name)
  }

  @JvmStatic
  public fun generatedJsonAdapterName(className: String): String {
    return "${className.replace("$", "_")}JsonAdapter"
  }

  @JvmStatic
  public fun nextAnnotations(
    annotations: Set<Annotation>,
    jsonQualifier: Class<out Annotation?>,
  ): Set<Annotation>? {
    require(jsonQualifier.isAnnotationPresent(JsonQualifier::class.java)) {
      "$jsonQualifier is not a JsonQualifier."
    }
    if (annotations.isEmpty()) return null

    for (annotation in annotations) {
      if ((jsonQualifier == (annotation as JavaAnnotation).annotationType())) {
        val delegateAnnotations = LinkedHashSet(annotations)
        delegateAnnotations.remove(annotation)
        return Collections.unmodifiableSet(delegateAnnotations)
      }
    }
    return null
  }

  @JvmStatic
  public fun newParameterizedType(rawType: Type, vararg typeArguments: Type): ParameterizedType {
    require(typeArguments.isNotEmpty()) { "Missing type arguments for $rawType" }
    return ParameterizedTypeImpl(null, rawType, *typeArguments)
  }

  @JvmStatic
  public fun newParameterizedTypeWithOwner(
    ownerType: Type?,
    rawType: Type,
    vararg typeArguments: Type,
  ): ParameterizedType {
    require(typeArguments.isNotEmpty()) { "Missing type arguments for $rawType" }
    return ParameterizedTypeImpl(ownerType, rawType, *typeArguments)
  }

  @JvmStatic
  public fun arrayOf(componentType: Type): GenericArrayType {
    return GenericArrayTypeImpl(componentType)
  }

  @JvmStatic
  public fun subtypeOf(bound: Type): WildcardType {
    val upperBounds = if (bound is WildcardType) bound.upperBounds else arrayOf<Type>(bound)
    return WildcardTypeImpl(upperBounds, EMPTY_TYPE_ARRAY)
  }

  @JvmStatic
  public fun supertypeOf(bound: Type): WildcardType {
    val lowerBounds = if (bound is WildcardType) bound.lowerBounds else arrayOf<Type>(bound)
    return WildcardTypeImpl(arrayOf<Type>(Any::class.java), lowerBounds)
  }

  @JvmStatic
  public fun getRawType(type: Type?): Class<*> {
    return when (type) {
      is Class<*> -> type
      is ParameterizedType -> type.rawType as Class<*>
      is GenericArrayType -> {
        val componentType = type.genericComponentType
        java.lang.reflect.Array.newInstance(getRawType(componentType), 0).javaClass
      }
      is TypeVariable<*> -> Any::class.java
      is WildcardType -> getRawType(type.upperBounds[0])
      else -> {
        val className = type?.javaClass?.name?.toString()
        throw IllegalArgumentException("Expected a Class, ParameterizedType, or GenericArrayType, but <$type> is of type $className")
      }
    }
  }

  @JvmStatic
  public fun collectionElementType(context: Type, contextRawType: Class<*>): Type {
    var collectionType: Type? = getSupertype(context, contextRawType, MutableCollection::class.java)
    if (collectionType is WildcardType) collectionType = collectionType.upperBounds[0]

    return if (collectionType is ParameterizedType) {
      collectionType.actualTypeArguments[0]
    } else Any::class.java
  }

  /** FIXED VERSION — returns Boolean correctly */
  @JvmStatic
  public fun equals(a: Type?, b: Type?): Boolean {
    if (a === b) return true

    return when (a) {
      is Class<*> -> {
        when {
          b is GenericArrayType ->
            equals(a.componentType, b.genericComponentType)

          b is ParameterizedType && a.rawType == b.rawType ->
            a.typeParameters.flatMap { it.bounds.toList() } ==
              b.actualTypeArguments.toList()

          else -> a == b
        }
      }

      is ParameterizedType -> {
        if (b is Class<*> && a.rawType == b.rawType) {
          return b.typeParameters.map { it.bounds }.toTypedArray().flatten() ==
            a.actualTypeArguments.toList()
        }
        if (b !is ParameterizedType) return false

        val aTypeArguments =
          if (a is ParameterizedTypeImpl) a.typeArguments else a.actualTypeArguments
        val bTypeArguments =
          if (b is ParameterizedTypeImpl) b.typeArguments else b.actualTypeArguments

        equals(a.ownerType, b.ownerType) &&
          (a.rawType == b.rawType) &&
          aTypeArguments.contentEquals(bTypeArguments)
      }

      is GenericArrayType -> {
        when (b) {
          is Class<*> -> equals(b.componentType, a.genericComponentType)
          is GenericArrayType -> equals(a.genericComponentType, b.genericComponentType)
          else -> false
        }
      }

      is WildcardType ->
        b is WildcardType &&
          a.upperBounds.contentEquals(b.upperBounds) &&
          a.lowerBounds.contentEquals(b.lowerBounds)

      is TypeVariable<*> ->
        b is TypeVariable<*> &&
          a.genericDeclaration === b.genericDeclaration &&
          a.name == b.name

      else -> false
    }
  }

  @Deprecated("This is no longer needed in Kotlin 1.6.0")
  @JvmStatic
  public fun getFieldJsonQualifierAnnotations(
    clazz: Class<*>,
    fieldName: String,
  ): Set<Annotation> {
    try {
      val field = clazz.getDeclaredField(fieldName)
      field.isAccessible = true
      val fieldAnnotations = field.declaredAnnotations

      return buildSet(fieldAnnotations.size) {
        for (annotation in fieldAnnotations) {
          val hasJsonQualifier = (annotation as JavaAnnotation)
            .annotationType()
            .isAnnotationPresent(JsonQualifier::class.java)
          if (hasJsonQualifier) add(annotation)
        }
      }
    } catch (e: NoSuchFieldException) {
      throw IllegalArgumentException(
        "Could not access field $fieldName on class ${clazz.canonicalName}",
        e,
      )
    }
  }

  @JvmStatic
  public fun <T : Annotation?> createJsonQualifierImplementation(annotationType: Class<T>): T {
    require(annotationType.isAnnotation) { "$annotationType must be an annotation." }
    require(annotationType.isAnnotationPresent(JsonQualifier::class.java)) {
      "$annotationType must have @JsonQualifier."
    }
    require(annotationType.declaredMethods.isEmpty()) {
      "$annotationType must not declare methods."
    }

    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
      annotationType.classLoader,
      arrayOf<Class<*>>(annotationType),
    ) { proxy, method, args ->
      when (method.name) {
        "annotationType" -> annotationType
        "equals" -> annotationType.isInstance(args[0])
        "hashCode" -> 0
        "toString" -> "@${annotationType.name}()"
        else -> method.invoke(proxy, *args)
      }
    } as T
  }

  @JvmStatic
  public fun mapKeyAndValueTypes(context: Type, contextRawType: Class<*>): Array<Type> {
    if (context === Properties::class.java)
      return arrayOf(String::class.java, String::class.java)

    val mapType = getSupertype(context, contextRawType, MutableMap::class.java)
    return if (mapType is ParameterizedType) mapType.actualTypeArguments
    else arrayOf(Any::class.java, Any::class.java)
  }

  @JvmStatic
  public fun getSupertype(context: Type, contextRawType: Class<*>, supertype: Class<*>): Type {
    if (!supertype.isAssignableFrom(contextRawType))
      throw IllegalArgumentException()
    return getGenericSupertype(context, contextRawType, supertype)
      .resolve(context, contextRawType)
  }

  @JvmStatic
  public fun getGenericSuperclass(type: Type): Type {
    val rawType = getRawType(type)
    return rawType.genericSuperclass.resolve(type, rawType)
  }

  @JvmStatic
  public fun arrayComponentType(type: Type): Type? {
    return when (type) {
      is GenericArrayType -> type.genericComponentType
      is Class<*> -> type.componentType
      else -> null
    }
  }
}
