package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.util.Log
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import javax.inject.Inject
import kotlin.math.atan2

class PageTemplateRescaler @Inject constructor(): IPageTemplateRescaler {
    override fun rescalePageOverlay(
        qrCorners: Array<Point>,
        pageTemplateBoundingBox: RectF,
        imageWidth: Float,
        imageHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): Path {

        val path = Path()

        qrCorners.let { if (it.size < 4) return path }

        // Calculate scale factors from image space to preview space
        val scaleX = previewWidth / imageWidth
        val scaleY = previewHeight / imageHeight

        Log.d("OVERLAY", "Scale: ${scaleX}x, ${scaleY}y")

        val qrRotationDegrees = calculateQRRotationAngle(qrCorners)
        Log.d("OVERLAY", "Rotation: $qrRotationDegrees°")

        val anchorPoint = qrCorners[3]
        Log.d("OVERLAY", "Anchor (image space): (${anchorPoint.x}, ${anchorPoint.y})")

        // Scale page dimensions to match preview
        val pageCorners = floatArrayOf(
            pageTemplateBoundingBox.left * scaleX, pageTemplateBoundingBox.top * scaleY,
            pageTemplateBoundingBox.right * scaleX, pageTemplateBoundingBox.top * scaleY,
            pageTemplateBoundingBox.right * scaleX, pageTemplateBoundingBox.bottom * scaleY,
            pageTemplateBoundingBox.left * scaleX, pageTemplateBoundingBox.bottom * scaleY
        )

        val matrix = Matrix()
        matrix.setRotate(qrRotationDegrees)
        matrix.mapPoints(pageCorners)

        // Scale anchor point to preview space
        val scaledAnchorX = anchorPoint.x * scaleX
        val scaledAnchorY = anchorPoint.y * scaleY

        for (i in pageCorners.indices step 2) {
            pageCorners[i] += scaledAnchorX
            pageCorners[i + 1] += scaledAnchorY
        }

        Log.d("OVERLAY", "Final page corners (preview space):")
        for (i in pageCorners.indices step 2) {
            Log.d("OVERLAY", "  (${pageCorners[i]}, ${pageCorners[i+1]})")
        }

        path.moveTo(pageCorners[0], pageCorners[1])
        path.lineTo(pageCorners[2], pageCorners[3])
        path.lineTo(pageCorners[4], pageCorners[5])
        path.lineTo(pageCorners[6], pageCorners[7])
        path.close()

        return path
    }

    /**
     * Calculate the rotation angle of the QR code from its corner points
     * Returns angle in degrees
     */
    private fun calculateQRRotationAngle(corners: Array<Point>): Float {
        if (corners.size < 2) return 0f

        // Use top edge (corner 0 to corner 1) to determine rotation
        val topLeft = corners[0]
        val topRight = corners[1]

        val deltaX = (topRight.x - topLeft.x).toFloat()
        val deltaY = (topRight.y - topLeft.y).toFloat()

        // Calculate angle in radians, then convert to degrees
        val angleRadians = atan2(deltaY, deltaX)
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

        return angleDegrees
    }

}