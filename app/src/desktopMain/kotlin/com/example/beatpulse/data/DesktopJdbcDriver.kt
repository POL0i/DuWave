package com.example.beatpulse.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

class DesktopJdbcSQLiteDriver : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection {
        val url = if (fileName.contains(":memory:")) fileName else "jdbc:sqlite:$fileName"
        val connection = DriverManager.getConnection(url)
        return DesktopJdbcSQLiteConnection(connection)
    }
}

class DesktopJdbcSQLiteConnection(private val connection: Connection) : SQLiteConnection {
    override fun close() {
        connection.close()
    }

    override fun prepare(sql: String): SQLiteStatement {
        println("Preparing SQL: $sql")
        return DesktopJdbcSQLiteStatement(connection, sql)
    }
}

class DesktopJdbcSQLiteStatement(
    private val connection: Connection,
    private val sql: String
) : SQLiteStatement {
    private var stmt: PreparedStatement? = null
    private var resultSet: ResultSet? = null

    private fun getStmt(): PreparedStatement {
        if (stmt == null) {
            stmt = connection.prepareStatement(sql)
        }
        return stmt!!
    }

    override fun bindBlob(index: Int, value: ByteArray) { getStmt().setBytes(index, value) }
    override fun bindDouble(index: Int, value: Double) { getStmt().setDouble(index, value) }
    override fun bindLong(index: Int, value: Long) { getStmt().setLong(index, value) }
    override fun bindNull(index: Int) { getStmt().setNull(index, java.sql.Types.NULL) }
    override fun bindText(index: Int, value: String) { getStmt().setString(index, value) }
    
    override fun clearBindings() {
        stmt?.clearParameters()
    }

    override fun getBlob(index: Int): ByteArray = resultSet!!.getBytes(index + 1)
    override fun getDouble(index: Int): Double = resultSet!!.getDouble(index + 1)
    override fun getLong(index: Int): Long = resultSet!!.getLong(index + 1)
    override fun getText(index: Int): String = resultSet!!.getString(index + 1)
    
    override fun isNull(index: Int): Boolean {
        resultSet!!.getObject(index + 1)
        return resultSet!!.wasNull()
    }
    
    override fun getColumnCount(): Int {
        return resultSet?.metaData?.columnCount ?: getStmt().metaData?.columnCount ?: 0
    }

    override fun getColumnName(index: Int): String {
        return resultSet?.metaData?.getColumnName(index + 1) ?: getStmt().metaData?.getColumnName(index + 1) ?: ""
    }

    override fun step(): Boolean {
        if (resultSet == null) {
            val upperSql = sql.trim().uppercase()
            val isResultSet = try {
                if (upperSql.startsWith("BEGIN")) {
                    connection.autoCommit = false
                    false
                } else if (upperSql.startsWith("COMMIT") || upperSql.startsWith("END")) {
                    connection.commit()
                    connection.autoCommit = true
                    false
                } else if (upperSql.startsWith("ROLLBACK")) {
                    if (!connection.autoCommit) {
                        connection.rollback()
                        connection.autoCommit = true
                    }
                    false
                } else {
                    getStmt().execute()
                }
            } catch (e: Exception) {
                if (upperSql.startsWith("ROLLBACK") || upperSql.startsWith("PRAGMA")) {
                    // Ignore PRAGMA or ROLLBACK errors
                    false
                } else {
                    throw e
                }
            }
            if (isResultSet) {
                resultSet = getStmt().resultSet
            }
        }
        return resultSet?.next() ?: false
    }

    override fun reset() {
        resultSet?.close()
        resultSet = null
    }

    override fun close() {
        resultSet?.close()
        stmt?.close()
    }
    
    override fun getColumnType(index: Int): Int {
        val obj = resultSet!!.getObject(index + 1)
        if (obj == null) return 5
        return when (obj) {
            is Long, is Int, is Short, is Byte -> 1
            is Double, is Float -> 2
            is String -> 3
            is ByteArray -> 4
            else -> 3
        }
    }
}
