package com.example.shouren.functions.createFunction

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//日历二维码模型  ->用于存储日历事件细信息，并支持与ICalendar(ics)格式字符串相互转换
data class CalendarModel(
    var title: String ="",
    var description: String = "",
    var location: String = "",
    var isAllDay: Boolean = false,
    var startTime: LocalDateTime = LocalDateTime.now(),
    var endTime: LocalDateTime = LocalDateTime.now().plusHours(1)
) {
    //iCalendar标准协议的关键字
    companion object{
        //格式化工具类
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        private val DATE_FORMATER = DateTimeFormatter.ofPattern("yyyyMMdd")

        //从一段格式文本中(icsContent)解析出CalendarCreateModel对象，如果解析失败或者缺少关键字段，返回null
        fun fromString(icsContent: String): CalendarModel?{
            //按文本进行分割，去掉多余空格，并过滤空行
            val lines = icsContent.lines().map{it.trim()}.filter { it.isNotEmpty() }

            var title = ""
            var description = ""
            var location = ""
            var startTimeStr: String? = null
            var endTimeStr: String ? =null
            var isAllDay: Boolean = false

            //遍历每一行，根据关键字提取信息
            for (line in lines) {
                when{
                    line.startsWith("SUMMARY:") ->{
                        title = line.substringAfter("SUMMARY:","")
                    }
                    line.startsWith("DESCRIPTION:") -> {
                        description = line.substringAfter("DESCRIPTION:", "")
                    }
                    line.startsWith("LOCATION:") -> {
                        location = line.substringAfter("LOCATION:", "")
                    }
                    // 全天事件的日期格式通常以 DTSTART;VALUE=DATE: 开头
                    line.startsWith("DTSTART;VALUE=DATE:") -> {
                        startTimeStr = line.substringAfter("DTSTART;VALUE=DATE:", "")
                        isAllDay = true
                    }
                    line.startsWith("DTEND;VALUE=DATE:") -> {
                        endTimeStr = line.substringAfter("DTEND;VALUE=DATE:", "")
                        isAllDay = true
                    }
                    // 普通带时间的格式
                    line.startsWith("DTSTART:") -> {
                        startTimeStr = line.substringAfter("DTSTART:", "")
                    }
                    line.startsWith("DTEND:") -> {
                        endTimeStr = line.substringAfter("DTEND:", "")
                    }
                }
            }
            //验证必要字段，标题与时间不能为空
            if (title.isBlank() ||startTimeStr == null || endTimeStr == null){
                return null
            }
            return try {
                if (isAllDay) {
                    // 解析全天日期 (只有年月日)
                    val startDate = LocalDate.parse(startTimeStr,DATE_FORMATER)
                    val endDate = LocalDate.parse(endTimeStr,DATE_FORMATER)
                    CalendarModel(
                        title = title,
                        description = description,
                        location = location,
                        isAllDay = true,
                        startTime = startDate.atStartOfDay(),
                        endTime = endDate.atStartOfDay()
                    )
                } else {
                    // 解析具体时间
                    CalendarModel(
                        title = title,
                        description = description,
                        location = location,
                        startTime = LocalDateTime.parse(startTimeStr,
                           DATETIME_FORMATTER
                        ),
                        endTime = LocalDateTime.parse(endTimeStr,
                           DATETIME_FORMATTER
                        )
                    )
                }
            }catch (_: Exception){
                null //如果时间格式不正确导致解析失败，忽略异常并返回null
            }
        }
    }

    //将当前对象转换成iCalendar对象 格式字符串，用于生成二维码
    fun getQRContent(): String{
        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//SHOUREN//Calendar QR Generator//EN\r\n")
            append("BEGIN:VEVENT\r\n")
            append("SUMMARY:$title\r\n")
            if (isAllDay){
                //全天事件输出日期格式
                append("DTSTART;VALUE=DATE:${startTime.format(DATE_FORMATER)}\r\n")
                append("DTEND;VALUE=DATE:${endTime.format(DATE_FORMATER)}\r\n")
            }else{
                //普通事件输出详细时间格式
                append("DTSTART:${startTime.format(DATETIME_FORMATTER)}\r\n")
                append("DTEND:${endTime.format(DATETIME_FORMATTER)}\r\n")
            }
            if (location.isNotBlank()){
                append("LOCATION:$location\r\n")
            }
            if (description.isNotBlank()){
                append("DESCRIPTION:$description\r\n")
            }
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
    }

    //获取标识符(通常是用作文件保存时的文件名) 默认为标题
    fun getID(): String{
        return title.ifBlank { "Untitled" }
    }

    //验证输入是否有效，且结束时间不能早于开始时间
    fun isValid(): Boolean{
        if (title.isBlank()) return false
        // 结束时间必须在开始时间之后（或相等，取决于业务逻辑，通常是之后）
        return !endTime.isBefore(startTime)
    }
}