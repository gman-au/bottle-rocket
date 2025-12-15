package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IBoundingBoxValidator {
    fun isValid(box: RocketBoundingBox): Boolean
    fun getValidationIssues(box: RocketBoundingBox): List<String>
}