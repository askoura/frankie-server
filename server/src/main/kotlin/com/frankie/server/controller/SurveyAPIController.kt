package com.frankie.server.controller

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.ext.JsonExt
import com.frankie.expressionmanager.model.*
import com.frankie.expressionmanager.usecase.DesignerInput
import com.frankie.expressionmanager.usecase.ValidationJsonOutput
import com.frankie.scriptengine.ScriptUtils
import com.frankie.server.data.EditResponseInput
import com.frankie.server.data.StartResponseInput
import com.frankie.server.data.domain.Survey
import com.frankie.server.processor.ApiNavigationOutput
import com.frankie.server.processor.SurveyProcessor
import com.frankie.server.processor.with
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import jakarta.inject.Inject


@Suppress("unused")
@Controller("/api")
class SurveyAPIController {

    @Inject
    lateinit var surveyEntityController: SurveyEntityController


    @Post("/init/sample_survey")
    fun initSampleSurvey(): HttpResponse<Survey> {
        val survey = Survey(
            name = "Sample Survey",
            defaultLang = SurveyLang.EN,
            additionalLanguages = listOf(SurveyLang.DE, SurveyLang.AR),
            navigationMode = NavigationMode.GROUP_BY_GROUP
        )
        val resultSurvey = surveyEntityController.save(survey)
        val classLoader = javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream("startup/sample_survey.json")!!
        val surveyNode: ObjectNode = jacksonKtMapper.readTree(inputStream.reader().readText()) as ObjectNode
        validate(resultSurvey.id!!, surveyNode)
        return HttpResponse.ok(resultSurvey)
    }

    @Post("/edit_components/{sid}")
    fun edit(sid: Long, @Body stateObj: ObjectNode): DesignerInput {
        val surveyNode = JsonExt.addChildren(stateObj["Survey"] as ObjectNode, "Survey", stateObj)
        val validationJsonOutput = validate(sid, surveyNode)
        val survey = surveyEntityController.get(sid)!!
        return validationJsonOutput.toDesignerInput(mutableListOf(survey.defaultLang).apply {
            survey.additionalLanguages?.let { addAll(it) }
        })

    }

    private fun validate(id: Long, @Body survey: ObjectNode): ValidationJsonOutput {
        val validationJsonOutput = SurveyProcessor.process(survey)
        return if (surveyEntityController.editProcessedComponents(id, validationJsonOutput)) {
            validationJsonOutput
        } else {
            throw IllegalStateException("FAILED!!!")
        }
    }

    @Get("/{sid}/runtime.js")
    @Produces("text/javascript")
    fun runtimeJs(sid: Long): String {
        return surveyEntityController.getProcessedComponents(sid)!!.script + "\n\n" + ScriptUtils().script
    }

    @Post("/{sid}/navigate")
    fun navigate(sid: Long, @Body useCaseInput: ApiUseCaseInput): ApiNavigationOutput {
        val survey = surveyEntityController.get(sid)!!
        val validationJsonOutput = surveyEntityController.getProcessedComponents(sid)!!
        val navigationDirection = useCaseInput.navigationDirection
        if (navigationDirection == NavigationDirection.Start) {
            val navigationUseCaseInput = NavigationUseCaseInput(
                values = useCaseInput.values,
                navigationInfo = NavigationInfo(
                    navigationDirection = useCaseInput.navigationDirection,
                    navigationMode = survey.navigationMode,
                    navigationIndex = null
                ),
                defaultLang = survey.defaultLang,
                lang = survey.checkMatchingReturnDefault(useCaseInput.lang?.toSurveyLang()),
            )
            val result = SurveyProcessor.navigate(validationJsonOutput, navigationUseCaseInput)
            val responseInput = StartResponseInput(
                lang = survey.checkMatchingReturnDefault(useCaseInput.lang?.toSurveyLang()),
                values = result.toSave,
                navigationIndex = result.navigationIndex
            )
            val responseId = surveyEntityController.createNewResponse(sid, responseInput)
            val lang = navigationUseCaseInput.lang ?: navigationUseCaseInput.defaultLang
            return result.with(
                responseId = responseId,
                lang = lang,
                mutableListOf(survey.defaultLang).apply {
                    survey.additionalLanguages?.let { addAll(it) }
                    remove(lang)
                }
            )
        } else {
            val responseId = useCaseInput.responseId!!
            val response = surveyEntityController.getResponse(sid, responseId)

            val navigationUseCaseInput = NavigationUseCaseInput(
                values = response.values.toMutableMap().apply {
                    putAll(useCaseInput.values)
                },
                navigationInfo = NavigationInfo(
                    navigationDirection = useCaseInput.navigationDirection,
                    navigationMode = survey.navigationMode,
                    navigationIndex = response.navigationIndex
                ),
                defaultLang = survey.defaultLang,
                lang = survey.checkMatchingReturnDefault(useCaseInput.lang?.toSurveyLang() ?: response.lang),
            )
            val navigationResult = SurveyProcessor.navigate(validationJsonOutput, navigationUseCaseInput)
            val lang = navigationUseCaseInput.lang ?: navigationUseCaseInput.defaultLang
            val result = navigationResult.with(
                responseId = responseId,
                lang = lang,
                mutableListOf(survey.defaultLang).apply {
                    survey.additionalLanguages?.let { addAll(it) }
                    remove(lang)
                }
            )
            surveyEntityController.editResponse(
                sid, responseId,
                EditResponseInput(
                    navigationIndex = result.navigationIndex,
                    values = navigationResult.toSave,
                    lang = navigationUseCaseInput.lang
                )
            )
            return result

        }


    }

}