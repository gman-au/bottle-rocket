package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.PageTemplate

interface IQrCodeTemplateMatcher {
    fun tryMatch(qrCode: String?): PageTemplate?
}