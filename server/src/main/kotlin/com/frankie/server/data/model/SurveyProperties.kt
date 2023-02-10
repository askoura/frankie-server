package com.frankie.server.data.model

/**
 * Survey properties
 *
 * @property allowPrev: Show previous button inside navigation
 * @property canResume: survey can be saved and resumed
 * @property canJump: allow users to jump between Groups/Questions (should imply that validation happens at the end)
 */
data class SurveyProperties(
        val allowPrev: Boolean = true,
        val skipInvalid: Boolean = false,
        val canResume: Boolean = true,
        val canJump: Boolean = true
)