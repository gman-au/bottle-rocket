package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IBoundingBoxStabilizer {
    fun stabilize(current: RocketBoundingBox): RocketBoundingBox
    fun reset()
    fun isStable(): Boolean
}