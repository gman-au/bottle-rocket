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

    /**
     * Calculate page bounds by extending QR box edges proportionally.
     * Much simpler and more stable than homography!
     *
     * @param qrBox The detected QR code bounding box (4 corners)
     * @param pageWidthInQrUnits How many QR-widths wide is the page? (e.g., 9.0)
     * @param pageHeightInQrUnits How many QR-heights tall is the page? (e.g., 13.0)
     * @param qrOffsetX How many QR-widths from left edge is the QR code? (e.g., 2.0 = QR starts 2 units from left)
     * @param qrOffsetY How many QR-heights from top edge is the QR code? (e.g., 3.0 = QR starts 3 units from top)
     * @return The page bounding box
     */
    fun calculatePageBoundsSimple(
        qrBox: RocketBoundingBox,
        pageWidthInQrUnits: Float,
        pageHeightInQrUnits: Float,
        qrOffsetX: Float = 0f,
        qrOffsetY: Float = 0f
    ): RocketBoundingBox {

        // Get the QR box edge vectors (these already contain perspective)
        val topEdge = PointF(
            qrBox.topRight.x - qrBox.topLeft.x,
            qrBox.topRight.y - qrBox.topLeft.y
        )

        val leftEdge = PointF(
            qrBox.bottomLeft.x - qrBox.topLeft.x,
            qrBox.bottomLeft.y - qrBox.topLeft.y
        )

        // Calculate page top-left by offsetting from QR top-left
        val pageTopLeft = PointF(
            qrBox.topLeft.x - (topEdge.x * qrOffsetX) - (leftEdge.x * qrOffsetY),
            qrBox.topLeft.y - (topEdge.y * qrOffsetX) - (leftEdge.y * qrOffsetY)
        )

        // Calculate the four page corners by scaling the edge vectors
        val pageTopRight = PointF(
            pageTopLeft.x + (topEdge.x * pageWidthInQrUnits),
            pageTopLeft.y + (topEdge.y * pageWidthInQrUnits)
        )

        val pageBottomLeft = PointF(
            pageTopLeft.x + (leftEdge.x * pageHeightInQrUnits),
            pageTopLeft.y + (leftEdge.y * pageHeightInQrUnits)
        )

        val pageBottomRight = PointF(
            pageTopLeft.x + (topEdge.x * pageWidthInQrUnits) + (leftEdge.x * pageHeightInQrUnits),
            pageTopLeft.y + (topEdge.y * pageWidthInQrUnits) + (leftEdge.y * pageHeightInQrUnits)
        )

        return RocketBoundingBox(
            topLeft = pageTopLeft,
            topRight = pageTopRight,
            bottomRight = pageBottomRight,
            bottomLeft = pageBottomLeft
        )
    }

    /**
     * Alternative: If your template gives you the page dimensions as a RocketBoundingBox
     * where the QR code is at (0,0) and the dimensions are in QR units
     */
    override fun calculatePageBoundsFromTemplate(
        qrBox: RocketBoundingBox,
        pageTemplate: RocketBoundingBox
    ): RocketBoundingBox {
        // Extract dimensions from template
        // Assuming template has QR at topLeft (0,0) with size 1x1
        val pageWidthInQrUnits = pageTemplate.topRight.x - pageTemplate.topLeft.x
        val pageHeightInQrUnits = pageTemplate.bottomLeft.y - pageTemplate.topLeft.y
        val qrOffsetX = -pageTemplate.topLeft.x // How far left is page edge from QR
        val qrOffsetY = -pageTemplate.topLeft.y // How far up is page edge from QR

        return calculatePageBoundsSimple(
            qrBox,
            pageWidthInQrUnits,
            pageHeightInQrUnits,
            qrOffsetX,
            qrOffsetY
        )
    }
}
