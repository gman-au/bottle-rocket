package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.toMatOfPoint2f
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import org.opencv.calib3d.Calib3d
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject

class PageTemplateRescaler @Inject constructor() : IPageTemplateRescaler {

    companion object {
        private const val TAG = "PageTemplateRescaler"
    }

    override fun calculatePageBounds(
        qrBoxActual: RocketBoundingBox,
        pageBoxIdeal: RocketBoundingBox,
        rotationAngle: Float
    ): RocketBoundingBox {

        val qrSize = 1.0

        // Source: axis-aligned square (what we want to map FROM)
        val qrBoxIdealPts =
            MatOfPoint2f(
                Point(0.0, 0.0),
                Point(qrSize, 0.0),
                Point(qrSize, qrSize),
                Point(0.0, qrSize)
            )

        val qrBoxActualPts = qrBoxActual.toMatOfPoint2f()
        val pageBoxIdealPts = pageBoxIdeal.toMatOfPoint2f()

        val homography =
            Calib3d
                .findHomography(
                    qrBoxIdealPts,
                    qrBoxActualPts,
                    Calib3d.RANSAC,
                    1.0
                )

        Log.d(TAG, "Homography matrix:\n${homography.dump()}")

        // Apply perspective transform
        val transformedPageMat = MatOfPoint2f()
        org.opencv.core.Core.perspectiveTransform(
            pageBoxIdealPts,
            transformedPageMat,
            homography
        )

        val transformedPoints = transformedPageMat.toArray()

        Log.d(TAG, "Page after perspective transform:")
        transformedPoints.forEachIndexed { i, pt ->
            Log.d(TAG, "  [$i]: (${pt.x}, ${pt.y})")
        }

        return RocketBoundingBox(
            topLeft = PointF(transformedPoints[0].x.toFloat(), transformedPoints[0].y.toFloat()),
            topRight = PointF(transformedPoints[1].x.toFloat(), transformedPoints[1].y.toFloat()),
            bottomRight = PointF(
                transformedPoints[2].x.toFloat(),
                transformedPoints[2].y.toFloat()
            ),
            bottomLeft = PointF(transformedPoints[3].x.toFloat(), transformedPoints[3].y.toFloat())
        )
    }
}
