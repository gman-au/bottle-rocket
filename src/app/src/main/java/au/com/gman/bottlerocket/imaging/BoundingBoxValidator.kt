package au.com.gman.bottlerocket.imaging

import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IBoundingBoxValidator
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class BoundingBoxValidator @Inject constructor() : IBoundingBoxValidator {

    private val maxRotationDegrees: Float = 25f
    private val maxPerspectiveRatio: Float = 0.5f


    override fun isValid(box: RocketBoundingBox): Boolean {
        return isRotationAcceptable(box) && isPerspectiveAcceptable(box)
    }

    override fun getValidationIssues(box: RocketBoundingBox): List<String> {
        val issues = mutableListOf<String>()

        if (!isRotationAcceptable(box)) {
            issues.add("Rotate camera to align with page (${getRotationDegrees(box).toInt()}° tilt)")
        }

        if (!isPerspectiveAcceptable(box)) {
            issues.add("Hold camera more directly above page")
        }

        return issues
    }

    private fun isRotationAcceptable(box: RocketBoundingBox): Boolean {
        val rotation = abs(getRotationDegrees(box))
        return rotation <= maxRotationDegrees
    }

    private fun isPerspectiveAcceptable(box: RocketBoundingBox): Boolean {
        // Check if opposite sides are similar length (minimal perspective distortion)
        val topLength = distance(box.topLeft, box.topRight)
        val bottomLength = distance(box.bottomLeft, box.bottomRight)
        val leftLength = distance(box.topLeft, box.bottomLeft)
        val rightLength = distance(box.topRight, box.bottomRight)

        val horizontalRatio = abs(topLength - bottomLength) / maxOf(topLength, bottomLength)
        val verticalRatio = abs(leftLength - rightLength) / maxOf(leftLength, rightLength)

        return horizontalRatio <= maxPerspectiveRatio && verticalRatio <= maxPerspectiveRatio
    }

    private fun getRotationDegrees(box: RocketBoundingBox): Float {
        val deltaX = box.topRight.x - box.topLeft.x
        val deltaY = box.topRight.y - box.topLeft.y
        val angleRadians = atan2(deltaY.toDouble(), deltaX.toDouble())
        return Math.toDegrees(angleRadians).toFloat()
    }

    private fun distance(p1: android.graphics.PointF, p2: android.graphics.PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
}