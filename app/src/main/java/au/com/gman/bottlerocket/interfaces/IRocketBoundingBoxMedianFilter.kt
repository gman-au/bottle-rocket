package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IRocketBoundingBoxMedianFilter {
    fun add(box: RocketBoundingBox): RocketBoundingBox

    fun reset()
}