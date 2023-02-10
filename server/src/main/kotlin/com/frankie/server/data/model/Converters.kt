package com.frankie.server.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.frankie.expressionmanager.model.ResponseField
import com.frankie.expressionmanager.model.SurveyLang
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.toSurveyLang
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class SurveyPropertiesConverter :
    AttributeConverter<SurveyProperties, String> {
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    override fun convertToDatabaseColumn(attribute: SurveyProperties): String {
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): SurveyProperties {
        return mapper.readValue(dbData, SurveyProperties::class.java)
    }
}

@Converter
class SurveyLangConverter :
    AttributeConverter<SurveyLang, String> {
    override fun convertToDatabaseColumn(attribute: SurveyLang?): String? {
        return attribute?.code
    }

    override fun convertToEntityAttribute(dbData: String?): SurveyLang? {
        return dbData?.toSurveyLang()
    }


}

@Converter
class SurveyLangListConverter :
    AttributeConverter<List<SurveyLang>, String> {
    override fun convertToDatabaseColumn(attribute: List<SurveyLang>?): String? {
        return attribute?.let {
            jacksonKtMapper.writeValueAsString(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<SurveyLang>? {
        return dbData?.let {
            jacksonKtMapper.readValue(dbData, jacksonTypeRef<List<SurveyLang>>())
        }
    }

}

@Converter
class ResponsesSchemaConverter :
    AttributeConverter<List<ResponseField>, String> {
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    override fun convertToDatabaseColumn(attribute: List<ResponseField>): String {
        return mapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<ResponseField> {
        return mapper.readValue(dbData, jacksonTypeRef<List<ResponseField>>())
    }
}