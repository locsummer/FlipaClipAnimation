package com.flipaclip.animation.data.model

import java.io.Serializable
import java.util.UUID

data class Frame(
    val id: String = UUID.randomUUID().toString(),
    var index: Int = 0,
    val layers: MutableList<Layer> = mutableListOf(Layer(name = "Layer 1")),
    var thumbnailPath: String? = null
) : Serializable {

    fun deepCopy(): Frame {
        return Frame(
            id = UUID.randomUUID().toString(),
            index = index,
            layers = layers.map { it.deepCopy() }.toMutableList(),
            thumbnailPath = null
        )
    }

    fun getActiveLayer(layerIndex: Int): Layer {
        return if (layerIndex in layers.indices) {
            layers[layerIndex]
        } else if (layers.isNotEmpty()) {
            layers[0]
        } else {
            val newLayer = Layer(name = "Layer 1")
            layers.add(newLayer)
            newLayer
        }
    }
}
