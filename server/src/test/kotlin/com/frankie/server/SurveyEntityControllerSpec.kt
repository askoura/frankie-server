package com.frankie.server

import com.frankie.server.data.domain.Survey
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*


@MicronautTest
class SurveyEntityControllerSpec {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient


    @Test
    fun savesAndFetchesSurvey() {
        val survey = Survey(name = "Test Survey")
        val request = HttpRequest.POST("/survey/save", survey)

        val result = client.toBlocking().retrieve(request, Survey::class.java)
        val result1 = client.toBlocking().retrieve(HttpRequest.GET<Survey>("/survey/${result.id}"), Survey::class.java)
        assertEquals(result, result1)
        assertTrue(
            client.toBlocking().retrieve(HttpRequest.DELETE<Boolean>("/survey/${result.id}"), Boolean::class.java)
        )

    }

    @Test
    fun savesFetchesAndEditsSurvey() {
        val survey = Survey(name = "Test Survey")
        val createRequest = HttpRequest.POST("/survey/save", survey)
        val result = client.toBlocking().retrieve(createRequest, Survey::class.java)
        val getRequest = HttpRequest.GET<Survey>("/survey/${result.id}")
        val result1 = client.toBlocking().retrieve(getRequest, Survey::class.java)
        assertEquals(result, result1)
        val result2 = result1.copy(name = "NEW Name")
        val editRequest = HttpRequest.PUT("/survey/${result.id}", result2)
        assertTrue(client.toBlocking().retrieve(editRequest, Boolean::class.java))
        val result3 = client.toBlocking().retrieve(getRequest, Survey::class.java)
        assertEquals(result3, result2)
        assertTrue(
            client.toBlocking().retrieve(HttpRequest.DELETE<Boolean>("/survey/${result.id}"), Boolean::class.java)
        )
    }
}
