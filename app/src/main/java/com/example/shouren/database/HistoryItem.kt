package com.example.shouren.database
enum class RecordType{
    YOUTUBE,CALENDAR,TEXT
}

//简化的历史记录数据模型，统一了数据库存储和界面显示
data class HistoryItem(
    var id:Long = -1L,
    val  title: String,
    val content: String,
    val format: RecordType,
    val timestamp:Long = System.currentTimeMillis(),
    var isSelected: Boolean  = false  //默认是普通模式
)

