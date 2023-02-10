package com.frankie.server.data

import io.micronaut.http.multipart.CompletedFileUpload
import java.io.File

interface ResponsesRepository {
    fun createTable(): Int
    fun deleteTable(): Int
    fun start(startResponseInput: StartResponseInput): Int
    fun edit(responseId: Int, editResponseInput: EditResponseInput): Int
    fun createResponsesDirectory(): Boolean
    fun deleteResponseDirectory(): Boolean
    fun getResponseByID(id: Int): ResponseRow
    fun getResults(): List<ResponseRow>
    fun getResponseUploadFile(responseId: Int, key: String): File
    fun saveResponseUploadFile(responseId: Int, key: String, file: CompletedFileUpload): ResponseUploadFile
}