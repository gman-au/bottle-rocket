package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IPageTemplateRescaler {
    fun calculatePageBoundsFromTemplate(
        qrBox: RocketBoundingBox,
        pageTemplate: RocketBoundingBox
    ): RocketBoundingBox
}