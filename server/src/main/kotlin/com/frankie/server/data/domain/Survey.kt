package com.frankie.server.data.domain


import com.frankie.expressionmanager.model.NavigationMode
import com.frankie.expressionmanager.model.ResponseField
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.server.data.ResponsesRepository
import com.frankie.server.data.ResponsesRepositoryImpl
import com.frankie.server.data.model.*
import java.util.*
import javax.persistence.*
import javax.sql.DataSource

/**
 * Survey
 *
 * @property id: Nullable long, id exists when survey exists inside DB
 * @property name: unique
 * @property defaultLang: default language
 * @property additionalLanguages: additional languages for the survey
 * @property startDate: nullable Date, referring to start date
 * @property endDate: nullable Date, referring to start date
 * @property props: Extra survey setup
 * @property responsesSchema: Schema for survey values to be submitted
 * @property processedComponents: link for processes Components JSON file
 * @property navigationMode: Survey Navigation Mode
 * @property status: Enum, either draft or active
 * @constructor Create empty Survey
 */

@Entity
@Table(name = "survey")
data class Survey(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    @Column(name = "name", nullable = false)
    val name: String = "",

    @Column(name = "default_lang", nullable = false, columnDefinition = "VARCHAR(5)")
    @Convert(converter = SurveyLangConverter::class)
    val defaultLang: SurveyLang = SurveyLang.EN,

    @Column(name = "additional_languages", columnDefinition = "TEXT")
    @Convert(converter = SurveyLangListConverter::class)
    val additionalLanguages: List<SurveyLang>? = null,

    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    val startDate: Date? = Date(),

    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    val endDate: Date? = Date(),

    @Column(name = "props", columnDefinition = "TEXT")
    @Convert(converter = SurveyPropertiesConverter::class)
    val props: SurveyProperties = SurveyProperties(),

    @Column(name = "responses_schema", columnDefinition = "TEXT")
    @Convert(converter = ResponsesSchemaConverter::class)
    val responsesSchema: List<ResponseField> = listOf(),

    @Column(name = "processed_components")
    val processedComponents: String? = null,

    @Enumerated
    @Column(name = "navigation_mode", columnDefinition = "smallint")
    val navigationMode: NavigationMode = NavigationMode.GROUP_BY_GROUP,

    @Enumerated
    @Column(columnDefinition = "smallint")
    val status: SurveyStatus = SurveyStatus.DRAFT
) {

    fun processedComponentsFileName(): String = "processed_$id.json"
    private fun responseTableName(): String = "responses_$id"
    fun checkMatchingReturnDefault(surveyLang: SurveyLang?): SurveyLang {
        return if (surveyLang == null) {
            defaultLang
        } else if (surveyLang == defaultLang || additionalLanguages?.contains(surveyLang) == true) {
            surveyLang
        } else {
            defaultLang
        }
    }

    fun responsesRepo(dataSource: DataSource): ResponsesRepository =
        ResponsesRepositoryImpl(dataSource, id!!, responseTableName(), responsesSchema)

}


enum class SurveyStatus {
    ACTIVE, DRAFT, CLOSED
}