package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject
import kotlin.collections.get

class QrCodeTemplateMatcher @Inject constructor(): IQrCodeTemplateMatcher {

    val templatesMap = mapOf(
        "04o" to PageTemplate(
            type = "1",
            // Page dimensions in QR-relative units
            // If QR is ~50px and page is 500x700px, that's 10x14 QR units
            pageDimensions = RocketBoundingBox(
                topLeft = PointF(0f, -220f),    // 21.5 QR-widths left, 28 QR-heights up
                topRight = PointF(350f, -220f),       // at QR X, 28 QR-heights up
                bottomRight = PointF(350f, 0f),      // at QR position (bottom-right corner)
                bottomLeft = PointF(0f, 0f)    // 21.5 QR-widths left, at QR Y
            )
        ),
        "P01 V1F T02 S000" to PageTemplate(
            type = "1",
            // Page dimensions in QR-relative units
            // If QR is ~50px and page is 500x700px, that's 10x14 QR units
            pageDimensions = RocketBoundingBox(
                topLeft = PointF(-20f, -700f),    // 21.5 QR-widths left, 28 QR-heights up
                topRight = PointF(350f, -700f),       // at QR X, 28 QR-heights up
                bottomRight = PointF(350f, 0f),      // at QR position (bottom-right corner)
                bottomLeft = PointF(-20f, 0f)    // 21.5 QR-widths left, at QR Y
            )
        )
    )

    override fun tryMatch(qrCode: String?): PageTemplate? {
        return if (qrCode in templatesMap) {
            templatesMap[qrCode]
        } else {
            null
        }
    }
}