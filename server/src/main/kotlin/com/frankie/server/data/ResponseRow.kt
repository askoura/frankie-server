package com.frankie.server.data

import com.fasterxml.jackson.annotation.JsonFormat
import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.model.SurveyLang
import java.util.*

/**
 * Response row
 *
 * @property id: Nullable int, not null when row exists in DB
 * @property navigationIndex: Current user location inside
 * @property startDate: Date for starting Survey
 * @property submitDate: Date for submitting Survey, not null when survey is submitted
 * @property lang: Starting lang for survey, (can be changed later and should be then saved inside Responses)
 * @property values: Survey values to be persisted
 * @constructor Create empty Response row
 */
data class ResponseRow(
    val id: Int? = null,
    val navigationIndex: NavigationIndex,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val startDate: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val submitDate: Date? = null,
    val lang: SurveyLang,
    val values: Map<String, Any> = mapOf()
)

data class StartResponseInput(
    val navigationIndex: NavigationIndex,
    val lang: SurveyLang,
    val values: Map<String, Any> = mapOf()
)

data class EditResponseInput(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val submitDate: Date? = null,
    val lang: SurveyLang? = null,
    val navigationIndex: NavigationIndex? = null,
    val values: Map<String, Any> = mapOf()
)

enum class FixedColumns(val colName: String) {
    ID("id"),
    NAV_INDEX("nav_index"),
    START_DATE("start_date"),
    SUBMIT_DATE("submit_date"),
    LANG("lang"),
    VALUES("user_values");

    fun colCommand(): String {
        return when (this) {
            ID -> "Int primary key AUTO_INCREMENT"
            NAV_INDEX -> "Text NOT NULL"
            START_DATE -> "TIMESTAMP NOT NULL"
            SUBMIT_DATE -> "TIMESTAMP"
            LANG -> "VARCHAR(5) NOT NULL"
            VALUES -> "TEXT"
        }
    }
}
