package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject
import kotlin.collections.get

class QrCodeTemplateMatcher @Inject constructor() : IQrCodeTemplateMatcher {

    val templatesMap = mapOf(
        "04o" to PageTemplate(
            type = "1",
            pageDimensions = RocketBoundingBox(
                topLeft = PointF(0f, -25.0f),
                topRight = PointF(15.0f, -25.0f),
                bottomRight = PointF(15.0f, 1.0f),
                bottomLeft = PointF(0f, 1.0f)
            )
        ),
        "P01 V1F T02 S000" to PageTemplate(
            type = "1",
            pageDimensions = RocketBoundingBox(
                topLeft = PointF(0f, -11.5F),
                topRight = PointF(9.5f, -11.5F),
                bottomRight = PointF(9.5f, 1.0F),
                bottomLeft = PointF(0F, 1.0F)
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