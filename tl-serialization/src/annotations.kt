@file:OptIn(ExperimentalSerializationApi::class)

package io.github.andreypfau.tl.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.SerialDescriptor

@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class TLCombinatorId(
    val id: Long
)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class TLConditional(
    val field: String,
    val value: Int
)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class TLFixedSize(
    val value: Int = -1,
    val field: String = ""
)

fun SerialDescriptor.getTlCombinatorId(): Int {
    for (i in annotations.indices) {
        val annotation = annotations[i]
        if (annotation is TLCombinatorId) {
            return annotation.id.toInt()
        }
    }
    error("No TLConstructorId annotation found for $serialName")
}

fun SerialDescriptor.getTlCombinatorId(element: Int): Int {
    val annotations = getElementAnnotations(element)

    for (i in annotations.indices) {
        val annotation = annotations[i]
        if (annotation is TLCombinatorId) {
            return annotation.id.toInt()
        }
    }
    error("No TLCombinatorId annotation found for $serialName")
}
