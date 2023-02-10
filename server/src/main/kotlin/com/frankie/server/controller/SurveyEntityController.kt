package com.frankie.server.controller

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.usecase.DesignerInput
import com.frankie.expressionmanager.usecase.ValidationJsonOutput
import com.frankie.server.data.*
import com.frankie.server.data.domain.Survey
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.annotation.Nonnull
import jakarta.inject.Inject


@Controller("/survey")
class SurveyEntityController {
    @Inject
    private lateinit var surveyRepository: SurveyRepository

    @Get("/{sid}")
    fun get(@Nonnull sid: Long): Survey? {
        return surveyRepository.findById(sid)
    }

    @Post("/save")
    fun save(@Body survey: Survey): Survey {
        return surveyRepository.save(survey)
    }

    @Put("/{sid}")
    fun edit(sid: Long, @Body survey: Survey): Boolean {
        surveyRepository.update(sid, survey)
        return true
    }

    @Delete("/{sid}")
    fun delete(@Nonnull sid: Long): Boolean {
        return surveyRepository.remove(sid) == 1
    }

    fun getProcessedComponents(@Nonnull sid: Long): ValidationJsonOutput? {
        val processedComponents = surveyRepository.getProcessedComponents(sid)
        return jacksonKtMapper.readValue(processedComponents, jacksonTypeRef<ValidationJsonOutput>())
    }

    @Get("/designer_input/{sid}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDesignerInput(@Nonnull sid: Long): DesignerInput? {
        val survey = get(sid)
        val processedComponents = getProcessedComponents(sid)
        return if (survey != null && processedComponents != null) {
            processedComponents.toDesignerInput(
                mutableListOf(survey.defaultLang).apply {
                    survey.additionalLanguages?.let { addAll(it) }
                }
            )
        } else
            null
    }

    @Put("/{sid}/processed_components")
    fun editProcessedComponents(@Nonnull sid: Long, @Body validationJsonOutput: ValidationJsonOutput): Boolean {
        return surveyRepository.saveProcessedComponents(sid, jacksonKtMapper.writeValueAsString(validationJsonOutput))
                && surveyRepository.saveSchema(sid, validationJsonOutput.schema)
    }


    fun createNewResponse(@Nonnull sid: Long, @Body responsesInput: StartResponseInput): Int {
        return surveyRepository.start(sid, responsesInput)
    }

    @Get("/{sid}/{rid}")
    fun getResponse(@Nonnull sid: Long, rid: Int): ResponseRow {
        return surveyRepository.getResponseByID(sid, rid)
    }

    @Put("/{sid}/{rid}")
    fun editResponse(@Nonnull sid: Long, @Nonnull rid: Int, @Body editResponseInput: EditResponseInput): Boolean {
        return surveyRepository.edit(sid, rid, editResponseInput) == 1
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/upload/{sid}/{rid}/{key}")
    fun uploadResponseFile(
        sid: String,
        rid: String,
        key: String,
        file: CompletedFileUpload
    ): ResponseUploadFile {
        return surveyRepository.saveResponseUploadFile(
            sid.toLong(),
            rid.toInt(),
            "$key.value",
            file
        )
    }

    @Suppress("UNUSED_PARAMETER")
    @Produces(MediaType.ALL)
    @Get("/{sid}/upload/{rid}/{key}/{filename}")
    fun getResponseFile(
        sid: Long,
        rid: Int,
        key: String,
        filename: String
    ): ByteArray {
        return surveyRepository.getResponseUploadFile(
            sid,
            rid,
            "$key.value",
        ).readBytes()
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/{sid}/resource/upload")
    fun uploadResourceFile(
        sid: String,
        file: CompletedFileUpload
    ): Boolean {
        return surveyRepository.saveResourceFile(
            sid.toLong(),
            file
        )
    }

    @Produces(MediaType.ALL)
    @Get("/{sid}/resource/{filename}")
    fun getResourceFile(
        sid: Long,
        filename: String
    ): ByteArray {
        return surveyRepository.getResourceFile(
            sid,
            filename
        ).readBytes()
    }


}