package com.frankie.server.data

import com.frankie.expressionmanager.model.*
import com.frankie.server.data.domain.Survey
import com.frankie.server.data.domain.SurveyStatus
import com.frankie.server.data.model.SurveyProperties
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*
import javax.sql.DataSource


@MicronautTest
class SurveyRepositoryTest {
    @Inject
    lateinit var dataSource: DataSource

    @Inject
    lateinit var surveyRepository: SurveyRepository


    lateinit var responsesRepository: ResponsesRepository

    @BeforeEach
    fun setup() {
        responsesRepository = ResponsesRepositoryImpl(
            dataSource,
            1,
            "test",
            listOf(
                ResponseField("Q1", ColumnName.VALUE, DataType.MAP),
                ResponseField("Q2", ColumnName.VALUE, DataType.LIST),
                ResponseField("Q3", ColumnName.VALUE, DataType.BOOLEAN),
                ResponseField("Q4", ColumnName.VALUE, DataType.INT),
                ResponseField("Q5", ColumnName.VALUE, DataType.STRING)
            )
        )
    }


    @Test
    fun `save and find survey`() {
        val survey1 = Survey(name = "ONE", startDate = Date())
        surveyRepository.save(survey1)
        assertEquals(survey1, surveyRepository.findById(survey1.id!!))
    }


    @Test
    fun `edit survey record`() {
        val survey1 = Survey(name = "ONE1", startDate = Date())
        surveyRepository.save(survey1)
        val survey2 = Survey(
            id = survey1.id,
            name = "TWO",
            startDate = Date(),
            endDate = Date(),
            status = SurveyStatus.ACTIVE,
            props = SurveyProperties(allowPrev = false)
        )
        surveyRepository.update(survey1.id!!, survey2)
        assertEquals(survey2, surveyRepository.findById(survey2.id!!))
    }

    @Test
    fun `add and edit response`() {
        val survey1 = Survey(name = "ONE2", startDate = Date())
        surveyRepository.save(survey1)
        val responsesSchema = listOf(
            ResponseField("Q1", ColumnName.VALUE, DataType.STRING)
        )
        surveyRepository.saveProcessedComponents(survey1.id!!, "{}")
        surveyRepository.saveSchema(survey1.id!!, responsesSchema)
        val startResponseInput = StartResponseInput(
            lang = SurveyLang.EN,
            values = mapOf("Q1.value" to "a770"),
            navigationIndex = NavigationIndex.Groups(listOf())
        )
        val responseId = surveyRepository.start(survey1.id!!, startResponseInput)
        val edited =
            EditResponseInput(submitDate = Date(), navigationIndex = startResponseInput.navigationIndex, lang = null)
        surveyRepository.edit(survey1.id!!, responseId, edited)

        val df = SimpleDateFormat("yyyy/MM/dd")
        assertEquals(responsesSchema, surveyRepository.findById(survey1.id!!)!!.responsesSchema)
        assertEquals(
            startResponseInput.values,
            surveyRepository.getResponseByID(survey1.id!!, responseId).values
        )
        assertEquals(
            df.format(edited.submitDate),
            df.format(surveyRepository.getResponseByID(survey1.id!!, responseId).submitDate)
        )
    }
}