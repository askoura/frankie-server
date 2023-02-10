package com.frankie.server

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.NavigationDirection
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.ApiUseCaseInput
import com.frankie.expressionmanager.usecase.DesignerInput
import com.frankie.expressionmanager.usecase.ValidationJsonOutput
import com.frankie.server.data.domain.Survey
import com.frankie.server.processor.ApiNavigationOutput
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File


@MicronautTest
class SurveyAPIControllerSpec {


    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    private val path = "${System.getProperty("user.dir")}/src/test/resources/"


    @Test
    fun testValidateAndNavigate() {
//        val surveyId = testValidate()
//        testNavigate(surveyId)
    }


    private fun testValidate(): Long {
        val request = HttpRequest.POST("/init/sample_survey", JsonNodeFactory.instance.objectNode())
        val survey = client.toBlocking().retrieve(request, Survey::class.java)

        val jsonInput =
            File("$path/sample_survey_validated.json").inputStream().readBytes()
                .toString(Charsets.UTF_8)
        val input = jacksonKtMapper.readValue(jsonInput, jacksonTypeRef<ValidationJsonOutput>())

        val getDefRequest = HttpRequest.GET<DesignerInput>("/survey/designer_input/${survey.id}")
        val resultDefComponents = client.toBlocking().retrieve(getDefRequest, DesignerInput::class.java)
//        assertEquals(input.schema, resultDefComponents.)
//        assertEquals(input.survey, resultDefComponents.survey)
//        assertEquals(input.impactMap.toSortedMap(), resultDefComponents.impactMap.toSortedMap())

        return survey.id!!
    }

    private fun testNavigate(surveyId: Long) {

        val useCaseInput = ApiUseCaseInput(
            navigationDirection = NavigationDirection.Start,
            lang = SurveyLang.EN.code,
            values = mapOf("Q1Aemail.value" to "aaa@a.a")
        )
        val request = HttpRequest.POST("/api/$surveyId/navigate", useCaseInput)

        val processed = client.toBlocking().retrieve(request, ApiNavigationOutput::class.java)

        val expectedJSON =
            File("$path/sample_survey_output.json").inputStream().readBytes()
                .toString(Charsets.UTF_8)
        val expectedInput = jacksonKtMapper.readValue(expectedJSON, jacksonTypeRef<ApiNavigationOutput>())

        assertEquals(expectedInput.survey, processed.survey)
        assertTrue(client.toBlocking().retrieve(HttpRequest.DELETE<Boolean>("/survey/$surveyId"), Boolean::class.java))

    }
}
