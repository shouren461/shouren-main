package com.example.shouren.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.fragment.app.FragmentActivity
import com.google.common.collect.Table
import org.checkerframework.dataflow.qual.Pure

class HistoryDBManagerHelper(context: FragmentActivity?, version: Int): SQLiteOpenHelper(context, DB_NAME,null,DB_VERSION) {
    companion object{
        const val DB_NAME = "history.db";
        private const val DB_VERSION = 1
        const val CREATE_TABLE_NAME = "createTable"
        const val SCAN_TABLE_NAME = "scanTable"
        const val ID = "id"
        const val CONTENT = "content"
        const val FORMAT = "format"
        const val TITLE = "title"
        const val TIMESTAMP = "timestamp"

    }
    override fun onCreate(db: SQLiteDatabase?) {
        //扫描历史记录表
        db?.execSQL(
            "CREATE TABLE ${SCAN_TABLE_NAME} (" +
                "${ID} TEXT ," +
                "${CONTENT} TEXT," +
                "${FORMAT} TEXT, "+
                "${TITLE} TEXT,"+
                "${TIMESTAMP} INTEGER )"
        )
        //创建历史记录表
        db?.execSQL( "CREATE TABLE ${CREATE_TABLE_NAME} (" +
                "${ID} TEXT ," +
                "${CONTENT} TEXT," +
                "${FORMAT} TEXT, "+
                "${TITLE} TEXT,"+
                "${TIMESTAMP} INTEGER )")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVerion: Int) {
        db?.execSQL("drop table if exists ${CREATE_TABLE_NAME}")
        db?.execSQL("drop table if exists ${SCAN_TABLE_NAME}")
        onCreate(db)
    }
    //实现数据管理

    //插入一条历史记录
    fun insert(tableName: String,item: HistoryItem){
        val values = ContentValues().apply {
            put(TITLE, item.title)
            put(CONTENT, item.content)
            put(FORMAT, item.format.toString())
            put(TIMESTAMP, item.timestamp)
        }
        writableDatabase.insert(tableName,null,values)
    }

    //查询某张表的所有记录(按时间倒序)
    fun getAll(tableName: String): List<HistoryItem>{
        val list = mutableListOf<HistoryItem>()
        val cursor =
            readableDatabase.query(tableName, null, null, null, null, null, "${TIMESTAMP} DESC")
        cursor?.use{
            while (it.moveToNext()){
                list.add(HistoryItem(
                    id = it.getLong(it.getColumnIndexOrThrow(ID)),
                    title = it.getString(it.getColumnIndexOrThrow(TITLE)),
                    content = it.getString(it.getColumnIndexOrThrow(CONTENT)),
                    format = RecordType.valueOf(it.getString(it.getColumnIndexOrThrow(FORMAT))),
                    timestamp =it.getLong(it.getColumnIndexOrThrow(TIMESTAMP))
                ))
            }
        }
        return list;
    }

    //批量删除历史记录
    fun deleteBatch(tableName: String,ids: List<Long>){
        if (ids.isEmpty()) return
        val idString = ids.joinToString(",")
        writableDatabase.execSQL("DELETE FROM $tableName WHERE $ID IN ($idString)")
    }

    //根据id查询单条历史记录
    fun select(id:Long, tableName: String): List<HistoryItem>{
        val list = mutableListOf<HistoryItem>()
        val cursor =
            readableDatabase.query(tableName, null, "$ID = ?", arrayOf(id.toString()), null, null, null)
        
        cursor?.use{
            while (it.moveToNext()){
                 list.add(HistoryItem(
                    id = it.getLong(it.getColumnIndexOrThrow(ID)),
                    title = it.getString(it.getColumnIndexOrThrow(TITLE)),
                    content = it.getString(it.getColumnIndexOrThrow(CONTENT)),
                    format = RecordType.valueOf(it.getString(it.getColumnIndexOrThrow(FORMAT))),
                    timestamp =it.getLong(it.getColumnIndexOrThrow(TIMESTAMP))
                ))
            }
        }
        return list;
    }

}