package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IBoundingBoxStabilizer
import javax.inject.Inject

class BoundingBoxStabilizer @Inject constructor(
    private val smoothingFactor: Float = 0.15f,
    private val requiredStableFrames: Int = 3
) : IBoundingBoxStabilizer {

    private var previousBounds: RocketBoundingBox? = null
    private var stableFrameCount = 0

    override fun stabilize(current: RocketBoundingBox): RocketBoundingBox {
        val previous = previousBounds

        val smoothed = if (previous == null) {
            current
        } else {
            smoothBounds(current, previous)
        }

        // Check if stable (not changing much)
        if (previous != null && isSimilar(smoothed, previous)) {
            stableFrameCount++
        } else {
            stableFrameCount = 1
        }

        previousBounds = smoothed
        return smoothed
    }

    override fun reset() {
        previousBounds = null
        stableFrameCount = 0
    }

    override fun isStable(): Boolean {
        return stableFrameCount >= requiredStableFrames
    }

    private fun smoothBounds(
        current: RocketBoundingBox,
        previous: RocketBoundingBox
    ): RocketBoundingBox {
        val alpha = smoothingFactor

        return RocketBoundingBox(
            topLeft = PointF(
                lerp(previous.topLeft.x, current.topLeft.x, alpha),
                lerp(previous.topLeft.y, current.topLeft.y, alpha)
            ),
            topRight = PointF(
                lerp(previous.topRight.x, current.topRight.x, alpha),
                lerp(previous.topRight.y, current.topRight.y, alpha)
            ),
            bottomRight = PointF(
                lerp(previous.bottomRight.x, current.bottomRight.x, alpha),
                lerp(previous.bottomRight.y, current.bottomRight.y, alpha)
            ),
            bottomLeft = PointF(
                lerp(previous.bottomLeft.x, current.bottomLeft.x, alpha),
                lerp(previous.bottomLeft.y, current.bottomLeft.y, alpha)
            )
        )
    }

    private fun isSimilar(a: RocketBoundingBox, b: RocketBoundingBox, threshold: Float = 5f): Boolean {
        return distance(a.topLeft, b.topLeft) < threshold &&
                distance(a.topRight, b.topRight) < threshold &&
                distance(a.bottomRight, b.bottomRight) < threshold &&
                distance(a.bottomLeft, b.bottomLeft) < threshold
    }

    private fun distance(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
}