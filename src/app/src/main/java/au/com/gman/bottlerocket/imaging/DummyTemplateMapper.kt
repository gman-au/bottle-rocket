package au.com.gman.bottlerocket.imaging

import au.com.gman.bottlerocket.domain.QRTemplateInfo
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.ITemplateMapper
import javax.inject.Inject

class DummyTemplateMapper @Inject constructor(): ITemplateMapper {
    override fun tryMatch(qrData: String): TemplateMatchResponse {
        val result = TemplateMatchResponse(matchFound = true, qrCode = QRTemplateInfo("", "", "", ""))
        return result
    }
}