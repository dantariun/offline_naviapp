package com.dantariun.sat_demo_map.util.common

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper


class DatabaseHelper(context: Context?, databaseName: String, databaseVersion:Int = 1) :
    SQLiteAssetHelper(context, databaseName, null, databaseVersion) {

    @SuppressLint("Range")
    fun readAllDataFromTable(db: SQLiteDatabase, tableName: String) {

        val cursor: Cursor = db.query(tableName, null, null, null, null, null, null)

        // val cursor: Cursor = db.rawQuery("SELECT * FROM $tableName", null)
        Log.d("DB_DATA", "$tableName")
        cursor.use { c ->
            // 데이터가 있는지 확인
            if (c.moveToFirst()) {
                do {
                    val rowData = StringBuilder()
                    // 모든 컬럼을 순회하며 데이터 추출
                    for (i in 0 until c.columnCount) {
                        val columnName = c.getColumnName(i)

                        Log.d("DB_DATA", "columnName : $columnName")
                        try {
                            val data = c.getString(c.getColumnIndex(columnName)) // 모든 데이터를 String으로 가져옴
                            rowData.append("$columnName: $data, ")
                        }catch (e: SQLiteException){

                            Log.d("DB_DATA", "SQLiteException - columnName : $columnName\n ${e.localizedMessage}")
                        }

                    }
                    Log.d("DB_DATA", "Row: [${rowData.toString().trimEnd(',', ' ')}]")

                } while (c.moveToNext()) // 다음 행으로 이동
            } else {
                Log.d("DB_DATA", "$tableName 테이블에 데이터가 없습니다.")
            }
        }
    }
}