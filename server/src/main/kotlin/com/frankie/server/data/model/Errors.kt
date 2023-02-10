package com.frankie.server.data.model

class WrongColumnException(columnName: String) : Exception("Wrong column name: $columnName")
class WrongValueType(columnName: String, expectedClassName: String, actualClassName: String) :
    Exception("Wrong value type for $columnName, expected $expectedClassName found $actualClassName")