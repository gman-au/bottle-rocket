package au.com.gman.bottlerocket.imaging

import android.graphics.RectF
import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject

class QrCodeTemplateMatcher @Inject constructor(): IQrCodeTemplateMatcher {

    val templatesMap = mapOf(
        "04o" to PageTemplate(
            type = "1",
            pageDimensions = RectF(
                -500f,  // left: 1000px to the left of QR
                -700f,  // top: 1400px above QR
                0f,      // right: at QR position
                0f       // bottom: at QR position
            )
        )
    )

    override fun tryMatch(qrCode: String?): PageTemplate? {
        return if (qrCode in templatesMap) {
            templatesMap[qrCode]!!
        } else {
            null
        }
    }
}