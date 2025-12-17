package au.com.gman.bottlerocket.extensions

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import androidx.core.graphics.toPointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun RocketBoundingBox.aggressiveSmooth(
    previous: RocketBoundingBox?,
    smoothFactor: Float = 0.7f,
    maxJumpThreshold: Float = 200f // Completely reject frames with jumps exceeding this
): RocketBoundingBox {
    if (previous == null) return this

    // Check if ANY corner has an unreasonable jump (likely homography instability)
    val jumps = listOf(
        distance(topLeft, previous.topLeft),
        distance(topRight, previous.topRight),
        distance(bottomRight, previous.bottomRight),
        distance(bottomLeft, previous.bottomLeft)
    )

    val maxJump = jumps.maxOrNull() ?: 0f

    // If ANY corner jumps too far, completely reject this frame
    if (maxJump > maxJumpThreshold) {
        return previous
    }

    // Normal smoothing for valid frames
    return RocketBoundingBox(
        topLeft = PointF(
            topLeft.x * (1f - smoothFactor) + previous.topLeft.x * smoothFactor,
            topLeft.y * (1f - smoothFactor) + previous.topLeft.y * smoothFactor
        ),
        topRight = PointF(
            topRight.x * (1f - smoothFactor) + previous.topRight.x * smoothFactor,
            topRight.y * (1f - smoothFactor) + previous.topRight.y * smoothFactor
        ),
        bottomRight = PointF(
            bottomRight.x * (1f - smoothFactor) + previous.bottomRight.x * smoothFactor,
            bottomRight.y * (1f - smoothFactor) + previous.bottomRight.y * smoothFactor
        ),
        bottomLeft = PointF(
            bottomLeft.x * (1f - smoothFactor) + previous.bottomLeft.x * smoothFactor,
            bottomLeft.y * (1f - smoothFactor) + previous.bottomLeft.y * smoothFactor
        )
    )
}

fun RocketBoundingBox.scaleWithOffset(scaleAndOffset: ScaleAndOffset): RocketBoundingBox {
    val offsetInSourceSpace = PointF(
        -scaleAndOffset.offset.x / scaleAndOffset.scale.x,
        -scaleAndOffset.offset.y / scaleAndOffset.scale.y
    )

    return RocketBoundingBox(
        topLeft = PointF(
            (topLeft.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (topLeft.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        topRight = PointF(
            (topRight.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (topRight.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        bottomRight = PointF(
            (bottomRight.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (bottomRight.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        bottomLeft = PointF(
            (bottomLeft.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (bottomLeft.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        )
    )
}

fun RocketBoundingBox.toFloatArray(): FloatArray {
    return floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )
}

fun RocketBoundingBox.toPath(): Path {
    val path = Path()

    path.moveTo(topLeft.x, topLeft.y)
    path.lineTo(topRight.x, topRight.y)
    path.lineTo(bottomRight.x, bottomRight.y)
    path.lineTo(bottomLeft.x, bottomLeft.y)
    path.close()

    return path
}

fun RocketBoundingBox.calculateRotationAngle(): Float {
    val deltaX = (topRight.x - topLeft.x)
    val deltaY = (topRight.y - topLeft.y)

    val angleRadians = atan2(deltaY, deltaX)
    val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

    return angleDegrees
}

fun RocketBoundingBox.applyRotation(angle: Float, pivot: PointF? = null): RocketBoundingBox {
    val actualPivot = pivot ?: PointF(
        (topLeft.x + bottomRight.x) / 2f,
        (topLeft.y + bottomRight.y) / 2f
    )

    val matrix = Matrix()
    matrix.setRotate(angle, actualPivot.x, actualPivot.y)

    val points = floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )

    matrix.mapPoints(points)

    return RocketBoundingBox(points)
}

fun RocketBoundingBox.round(): RocketBoundingBox {
    return RocketBoundingBox(
        topLeft = Point(topLeft.x.roundToInt(), topLeft.y.roundToInt()).toPointF(),
        topRight = Point(topRight.x.roundToInt(), topRight.y.roundToInt()).toPointF(),
        bottomRight = Point(bottomRight.x.roundToInt(), bottomRight.y.roundToInt()).toPointF(),
        bottomLeft = Point(bottomLeft.x.roundToInt(), bottomLeft.y.roundToInt()).toPointF()
    )
}

fun toPointArray(points: Array<out Point>?): Array<Point> {
    return points?.toList()?.toTypedArray() ?: arrayOf()
}

private fun distance(p1: PointF, p2: PointF): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt(dx * dx + dy * dy)
}
