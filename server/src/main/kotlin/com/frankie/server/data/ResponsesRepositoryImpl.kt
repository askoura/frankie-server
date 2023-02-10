package com.frankie.server.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.frankie.expressionmanager.model.NavigationIndex
import com.frankie.expressionmanager.model.ResponseField
import com.frankie.expressionmanager.model.jacksonKtMapper
import com.frankie.expressionmanager.model.toSurveyLang
import io.micronaut.http.multipart.CompletedFileUpload
import org.json.JSONObject
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.*
import javax.sql.DataSource

open class ResponsesRepositoryImpl(
    dataSource: DataSource,
    private val sid: Long,
    private val tableName: String,
    private val responsesSchema: List<ResponseField>
) : ResponsesRepository {
    private val connection: Connection = dataSource.connection


    override fun createTable(): Int {
        val stringBuilder = StringBuilder("CREATE TABLE IF NOT EXISTS $tableName ( ")
        stringBuilder.append(
            FixedColumns.values().joinToString(separator = ", ",
                transform = { "${it.colName} ${it.colCommand()}" })
        )

        stringBuilder.append(")")
        val stmt = connection.createStatement()
        return stmt.executeUpdate(stringBuilder.toString())
    }

    override fun deleteTable(): Int {
        val dropStmt = "DROP TABLE $tableName "
        val stmt = connection.createStatement()
        return stmt.executeUpdate(dropStmt)
    }

    override fun start(startResponseInput: StartResponseInput): Int {
        val stmt = prepareAndExecute(
            startResponseInput.values.validateSchema(responsesSchema),
            navigationIndex = startResponseInput.navigationIndex,
            langCode = startResponseInput.lang.code,
            startDate = Date()
        )
        stmt.executeUpdate()
        val generatedKeys = stmt.generatedKeys
        generatedKeys.next()
        return generatedKeys.getInt("id")
    }

    private fun prepareAndExecute(
        values: String,
        navigationIndex: NavigationIndex?,
        langCode: String?,
        submitDate: Date? = null,
        startDate: Date? = null,
        isEdit: Boolean = false,
        responseId: String? = null
    ): PreparedStatement {
        val addedValueList = mutableListOf<ValueItem>().apply {
            navigationIndex?.let {
                add(
                    ValueItem(
                        columnName = FixedColumns.NAV_INDEX.colName,
                        setValue = { statement: PreparedStatement, index: Int ->
                            statement.setString(index, jacksonKtMapper.writeValueAsString(navigationIndex))
                        })
                )
            }
            langCode?.let {
                add(
                    ValueItem(
                        columnName = FixedColumns.LANG.colName,
                        setValue = { statement: PreparedStatement, index: Int ->
                            statement.setString(index, langCode)
                        })
                )
            }
            submitDate?.let {
                add(
                    ValueItem(
                        columnName = FixedColumns.SUBMIT_DATE.colName,
                        setValue = { statement: PreparedStatement, index: Int ->
                            statement.setTimestamp(index, SqlTimeStamp(submitDate.time))
                        })
                )
            }
            startDate?.let {
                add(
                    ValueItem(
                        columnName = FixedColumns.START_DATE.colName,
                        setValue = { statement: PreparedStatement, index: Int ->
                            statement.setTimestamp(index, SqlTimeStamp(startDate.time))
                        })
                )
            }
            add(
                ValueItem(
                    columnName = FixedColumns.VALUES.colName,
                    setValue = { statement: PreparedStatement, index: Int ->
                        statement.setString(index, values)
                    })
            )
        }

        val stringBuilder = if (isEdit) {
            val updateValuesStatement =
                addedValueList.joinToString(separator = ", ", transform = { "${it.columnName} = ?" })
            "UPDATE $tableName SET $updateValuesStatement WHERE ${FixedColumns.ID.colName} = $responseId"
        } else {
            val colNames = addedValueList.joinToString(separator = ",", transform = { it.columnName })
            val colValues = addedValueList.joinToString(separator = ",", transform = { "?" })
            StringBuilder("INSERT INTO ${tableName}($colNames) VALUES($colValues)")
        }
        val stmt = connection.prepareStatement(
            stringBuilder.toString(),
            if (isEdit) Statement.NO_GENERATED_KEYS else Statement.RETURN_GENERATED_KEYS
        )
        addedValueList.forEachIndexed { index, valueItem ->
            valueItem.setValue(stmt, index + 1)
        }

        return stmt
    }

    override fun edit(responseId: Int, editResponseInput: EditResponseInput): Int {
        val response = getResponseByID(responseId)
        val finalValues = response.values.toMutableMap().apply {
            putAll(editResponseInput.values)
        }
        return prepareAndExecute(
            finalValues.validateSchema(responsesSchema),
            isEdit = true, responseId = responseId.toString(),
            navigationIndex = editResponseInput.navigationIndex,
            submitDate = editResponseInput.submitDate,
            langCode = editResponseInput.lang?.code,
        ).executeUpdate()
    }

    override fun createResponsesDirectory(): Boolean {
        val directory = File(sid.toString())
        if (!directory.exists()) {
            directory.mkdir()
        }
        val responses = File(directory, "responses")
        return responses.isDirectory || responses.mkdir()
    }

    override fun deleteResponseDirectory(): Boolean {
        return try {
            File(responsesFolderPath).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    override fun getResponseByID(id: Int): ResponseRow {
        val stmt = connection.createStatement()

        val resultSet = stmt.executeQuery("SELECT * FROM $tableName WHERE ${FixedColumns.ID.colName} = $id")
        resultSet.next()
        return resultSet.toResponseRow(responsesSchema)
    }


    override fun getResults(): List<ResponseRow> {
        val returnList = mutableListOf<ResponseRow>()
        val stmt = connection.createStatement()

        val resultSet = stmt.executeQuery("SELECT * FROM $tableName")
        while (resultSet.next()) {
            val responseRow = resultSet.toResponseRow(responsesSchema)
            returnList.add(responseRow)
        }
        return returnList
    }

    override fun getResponseUploadFile(responseId: Int, key: String): File {
        val response = getResponseByID(responseId)
        return File(responsesFolderPath, (response.values[key] as ResponseUploadFile).storedFilename)
    }

    override fun saveResponseUploadFile(responseId: Int, key: String, file: CompletedFileUpload): ResponseUploadFile {
        val random = UUID.randomUUID().toString()
        val responseUploadFile = ResponseUploadFile(file.filename, random, file.size)
        File(responsesFolderPath, random).writeBytes(file.bytes)
        val editResponseInput =
            EditResponseInput(
                values = mapOf(key to JSONObject(jacksonKtMapper.writeValueAsString(responseUploadFile)))
            )
        edit(responseId, editResponseInput)
        return responseUploadFile
    }

    private val responsesFolderPath = "$sid/responses"
}

data class ResponseUploadFile(
    val filename: String,
    @JsonProperty("stored_filename")
    val storedFilename: String,
    val size: Long
)

private fun ResultSet.toResponseRow(responsesSchema: List<ResponseField>): ResponseRow {
    return ResponseRow(
        id = getInt(FixedColumns.ID.colName),
        navigationIndex = jacksonKtMapper.readValue(getString(FixedColumns.NAV_INDEX.colName), jacksonTypeRef()),
        startDate = getTimestamp(FixedColumns.START_DATE.colName),
        submitDate = getTimestamp(FixedColumns.SUBMIT_DATE.colName),
        lang = getString(FixedColumns.LANG.colName).toSurveyLang(),
        values = toValuesMap(responsesSchema)
    )
}


private fun ResultSet.toValuesMap(responsesSchema: List<ResponseField>): Map<String, Any> {
    val returnMap = mutableMapOf<String, Any>()
    val values = getString(FixedColumns.VALUES.colName)
    if (wasNull()) {
        return returnMap
    }
    try {
        val json = JSONObject(values)
        responsesSchema.forEach { responseField ->
            json.get(responseField)?.let {
                returnMap[it.first] = it.second
            }
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

    return returnMap
}


typealias SqlTimeStamp = java.sql.Timestamp


data class ValueItem(
    val columnName: String,
    val setValue: (statement: PreparedStatement, index: Int) -> Unit
)