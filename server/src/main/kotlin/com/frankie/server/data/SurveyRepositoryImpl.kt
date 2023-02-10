package com.frankie.server.data

import com.frankie.expressionmanager.model.ResponseField
import com.frankie.server.data.domain.Survey
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.transaction.annotation.ReadOnly
import jakarta.inject.Singleton
import org.json.JSONObject
import java.io.File
import javax.persistence.EntityManager
import javax.sql.DataSource
import javax.transaction.Transactional

@Suppress("unused")
@Singleton
open class SurveyRepositoryImpl(
    private val entityManager: EntityManager,
    private val dataSource: DataSource
) : SurveyRepository {


    @ReadOnly
    override fun findById(id: Long): Survey? {
        return entityManager.find(Survey::class.java, id)
    }

    @Transactional
    override fun save(survey: Survey): Survey {
        entityManager.persist(survey)
        createResourceDirectory(survey.id!!)
        createTable(survey.id!!)
        createResponsesDirectory(survey.id!!)
        return survey
    }

    private fun refreshById(id: Long?) {
        id?.let {
            findById(id)?.apply {
                entityManager.refresh(this)
            }
        }
    }


    @Transactional
    override fun update(id: Long, survey: Survey) {
        findById(id)?.let {
            entityManager.merge(survey.copy(id = id))
        }

    }

    @Transactional
    override fun remove(id: Long): Int {
        getProcessedComponentsFile(id)?.delete()
        deleteTable(id)
        deleteResponseDirectory(id)
        return entityManager.createQuery("DELETE FROM Survey WHERE id = :id")
            .setParameter("id", id)
            .executeUpdate()
    }

    override fun createResourceDirectory(id: Long): Boolean {
        val directory = File(id.toString())
        if (!directory.exists()) {
            directory.mkdir()
        }
        val resources = File(directory, "resources")
        return resources.isDirectory || resources.mkdir()
    }


    @Transactional
    override fun saveSchema(surveyId: Long, schema: List<ResponseField>): Boolean {
        return findById(surveyId)?.let { survey ->
            val newSurvey = survey.copy(responsesSchema = schema)
            val resultSurvey = entityManager.merge(newSurvey)
            resultSurvey == newSurvey
        } ?: false
    }

    @Transactional
    override fun saveProcessedComponents(surveyId: Long, fileContents: String): Boolean {
        findById(surveyId)?.let { survey ->
            val fileName = survey.processedComponentsFileName()
            val file = File(fileName).apply {
                writeText(JSONObject(fileContents).toString(4))
            }
            val newSurvey = survey.copy(processedComponents = file.absolutePath)
            entityManager.merge(newSurvey)
        }
        return true
    }


    private fun getProcessedComponentsFile(surveyId: Long): File? {
        return findById(surveyId)?.let { survey ->
            survey.processedComponents?.let {
                File(it)
            }
        }
    }

    override fun getProcessedComponents(surveyId: Long): String? {
        return getProcessedComponentsFile(surveyId)?.readText()
    }

    override fun saveResourceFile(sid: Long, file: CompletedFileUpload): Boolean {
        val resourceDirectory = File("$sid/resources")
        File(resourceDirectory, file.filename).writeBytes(file.bytes)
        return true
    }

    override fun getResourceFile(sid: Long, filename: String): File {
        val resourceDirectory = File("$sid/resources")
        return File(resourceDirectory, filename)
    }

    private fun createTable(surveyId: Long): Int {
        return findById(surveyId)?.responsesRepo(dataSource)?.createTable() ?: 0
    }

    private fun deleteTable(surveyId: Long): Int {
        return findById(surveyId)?.responsesRepo(dataSource)?.deleteTable() ?: 0
    }

    private fun createResponsesDirectory(surveyId: Long): Boolean {
        return findById(surveyId)?.responsesRepo(dataSource)?.createResponsesDirectory() ?: false
    }

    private fun deleteResponseDirectory(surveyId: Long): Boolean {
        return findById(surveyId)?.responsesRepo(dataSource)?.deleteResponseDirectory() ?: false
    }

    @Transactional
    override fun start(surveyId: Long, startResponseInput: StartResponseInput): Int {
        return findById(surveyId)?.let { survey ->
            if (survey.defaultLang != startResponseInput.lang
                && survey.additionalLanguages?.contains(startResponseInput.lang)?.not() == true
            ) {
                throw IllegalArgumentException("asking for lang that doesn't exits!!")
            }
            survey.responsesRepo(dataSource).start(startResponseInput)
        } ?: -1
    }

    @Transactional
    override fun edit(surveyId: Long, responseId: Int, editResponseInput: EditResponseInput): Int {
        return findById(surveyId)?.responsesRepo(dataSource)?.edit(responseId, editResponseInput) ?: 0
    }

    @Transactional
    override fun getResponseByID(surveyId: Long, id: Int): ResponseRow {
        return findById(surveyId)?.responsesRepo(dataSource)?.getResponseByID(id)!!
    }

    @Transactional
    override fun getResults(surveyId: Long): List<ResponseRow> {
        return findById(surveyId)?.responsesRepo(dataSource)?.getResults()!!
    }

    @Transactional
    override fun getResponseUploadFile(surveyId: Long, responseId: Int, key: String): File {
        return findById(surveyId)?.responsesRepo(dataSource)?.getResponseUploadFile(responseId, key)!!
    }

    @Transactional
    override fun saveResponseUploadFile(
        surveyId: Long,
        responseId: Int,
        key: String,
        file: CompletedFileUpload,
    ): ResponseUploadFile {
        return findById(surveyId)?.responsesRepo(dataSource)?.saveResponseUploadFile(responseId, key, file)!!
    }

    companion object {
        private val VALID_PROPERTY_NAMES = listOf("id", "name")
    }
}