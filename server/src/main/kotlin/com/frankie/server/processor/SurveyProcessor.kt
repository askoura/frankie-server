package com.frankie.server.processor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.model.NavigationUseCaseInput
import com.frankie.expressionmanager.usecase.*
import com.frankie.scriptengine.ScriptEngineWrapper

object SurveyProcessor {

    val scriptEngineWrapper = ScriptEngineWrapper()

    val scriptEngine = object :ScriptEngine{
        override fun executeScript(method: String, script: String): String {
            return scriptEngineWrapper.executeScript(method, script)
        }
    }
    fun process(survey: ObjectNode): ValidationJsonOutput {
        val useCase = ValidationUseCaseWrapperImpl(scriptEngine, survey.toString())
        return useCase.validate()
    }

    fun navigate(
        validationJsonOutput: ValidationJsonOutput,
        useCaseInput: NavigationUseCaseInput
    ): NavigationJsonOutput {
        val useCase = NavigationUseCaseWrapperImpl(scriptEngine, validationJsonOutput, useCaseInput)
        return useCase.navigate()
    }
}

data class ApiNavigationOutput(
    val survey: ObjectNode,
    val state: ObjectNode,
    val navigationIndex: NavigationIndex,
    val responseId: Int,
    val lang: SurveyLang,
    val additionalLang: List<SurveyLang>?
)

fun NavigationJsonOutput.with(responseId: Int, lang: SurveyLang, additionalLang: List<SurveyLang>)
        : ApiNavigationOutput {
    return ApiNavigationOutput(survey, state, navigationIndex, responseId, lang, additionalLang)
}
