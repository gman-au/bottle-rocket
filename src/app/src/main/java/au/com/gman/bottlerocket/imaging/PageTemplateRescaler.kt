package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import javax.inject.Inject

class PageTemplateRescaler @Inject constructor() : IPageTemplateRescaler {

    companion object {
        private const val TAG = "PageTemplateRescaler"
    }

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
}
