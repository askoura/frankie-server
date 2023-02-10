package com.frankie.server.data

import com.frankie.expressionmanager.model.ResponseField
import com.frankie.server.data.domain.Survey
import io.micronaut.http.multipart.CompletedFileUpload
import java.io.File

interface SurveyRepository : GenericResponsesRepository {
    fun findById(id: Long): Survey?
    fun save(survey: Survey): Survey
    fun update(id: Long, survey: Survey)
    fun remove(id: Long): Int
    fun createResourceDirectory(id: Long): Boolean
    fun saveProcessedComponents(surveyId: Long, fileContents: String): Boolean
    fun saveSchema(surveyId: Long, schema: List<ResponseField>): Boolean
    fun getProcessedComponents(surveyId: Long): String?
    fun saveResourceFile(sid: Long, file: CompletedFileUpload): Boolean
    fun getResourceFile(sid: Long, filename: String): File
}

interface GenericResponsesRepository {
    fun start(surveyId: Long, startResponseInput: StartResponseInput): Int
    fun edit(surveyId: Long, responseId: Int, editResponseInput: EditResponseInput): Int
    fun getResponseByID(surveyId: Long, id: Int): ResponseRow
    fun getResults(surveyId: Long): List<ResponseRow>
    fun getResponseUploadFile(surveyId: Long, responseId: Int, key: String): File
    fun saveResponseUploadFile(
        surveyId: Long,
        responseId: Int,
        key: String,
        file: CompletedFileUpload
    ): ResponseUploadFile
}