package com.frankie.server.data

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.DataType
import com.frankie.expressionmanager.model.ResponseField
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.server.data.model.WrongColumnException
import com.frankie.server.data.model.WrongValueType
import org.json.JSONArray
import org.json.JSONObject

// this will be to retrieve values and pass it on to Expression Manager
fun JSONObject.get(responseField: ResponseField): Pair<String, Any>? {
    val colName = responseField.toColumnName()
    val sqlValue = if (!has(colName)) null else when (responseField.dataType) {
        DataType.BOOLEAN -> {
            getBoolean(colName)
        }

        DataType.STRING, DataType.LIST, DataType.MAP, DataType.DATE,
        DataType.FILE -> getString(colName)

        DataType.INT -> getInt(colName)
        DataType.DOUBLE -> getDouble(colName)
    }
    return if (sqlValue == null) null else responseField.toValueKey() to when (responseField.dataType) {
        DataType.MAP -> jacksonKtMapper.readValue(sqlValue as String, jacksonTypeRef<HashMap<String, Any>>())
        DataType.FILE -> jacksonKtMapper.readValue(sqlValue as String, jacksonTypeRef<ResponseUploadFile>())
        DataType.LIST -> jacksonKtMapper.readValue(sqlValue as String, jacksonTypeRef<List<Any>>())
        else -> sqlValue
    }
}

fun validateType(value: Any, dataType: DataType): Boolean {
    return when (dataType) {
        DataType.BOOLEAN -> value is Boolean
        DataType.DATE -> value is String
        DataType.STRING -> value is String
        DataType.DOUBLE -> value is Number
        DataType.INT -> value is Int
        DataType.LIST -> value is JSONArray
        DataType.FILE,
        DataType.MAP -> value is JSONObject
    }
}

fun expectedType(dataType: DataType): String {
    return when (dataType) {
        DataType.BOOLEAN -> Boolean::class.java.name
        DataType.DATE -> String::class.java.name
        DataType.STRING -> String::class.java.name
        DataType.DOUBLE -> Number::class.java.name
        DataType.INT -> Int::class.java.name
        DataType.LIST -> JSONArray::class.java.name
        DataType.FILE,
        DataType.MAP -> JSONObject::class.java.name
    }
}

fun Map<String, Any>.validateSchema(responsesSchema: List<ResponseField>): String {
    val jsonObject = JSONObject()
    forEach { entry ->
        val responseField = responsesSchema.firstOrNull { it.toValueKey() == entry.key }
        if (responseField == null) {
            throw WrongColumnException(entry.key)
        } else if (!validateType(entry.value, responseField.dataType)) {
            throw WrongValueType(
                columnName = entry.key,
                expectedClassName = expectedType(responseField.dataType),
                actualClassName = entry.value.javaClass.name
            )
        } else {
            jsonObject.put(
                responseField.toColumnName(),
                when (responseField.dataType) {
                    DataType.BOOLEAN -> entry.value as Boolean
                    DataType.DOUBLE -> (entry.value as Number).toDouble()
                    DataType.INT -> entry.value as Int
                    DataType.DATE,
                    DataType.STRING,
                    DataType.LIST,
                    DataType.FILE,
                    DataType.MAP -> entry.value.toString()
                }
            )
        }
    }
    return jsonObject.toString()
}

fun ResponseField.toColumnName(): String = "${componentCode}.${columnName.name.lowercase()}"