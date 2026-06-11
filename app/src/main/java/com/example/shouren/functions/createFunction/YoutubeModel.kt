package com.example.shouren.functions.createFunction

enum class YoutubeType{
    VIDEO,    //视频ID
    CHANNEL,  //频道ID
    URL      //完整URL
}
//创建Youtube输入页面的Model模型视图
data class YoutubeModel(
    var type: YoutubeType = YoutubeType.VIDEO,
    var input: String = ""
){
    companion object{
        private const val YOUTUBE_URL = "https://www.youtube.com/watch?v="

        private const val YOUTUBE_CHANNEL = "https://www.youtube.com/"
        //判断是否是Youtube链接
        public fun isYoutubeLink(text: String): Boolean{
            val lowercase = text.lowercase();
            return lowercase.contains("youtube.com")
        }

        // 自动识别 Youtube 链接类型
        fun identifyYoutubeType(text: String): YoutubeType {
            return when {
                text.contains("watch?v=") -> YoutubeType.VIDEO
                text.contains("@") || text.contains("/channel/") -> YoutubeType.CHANNEL
                else -> YoutubeType.URL
            }
        }
    }
    //获取完整二维码链接
    fun getContent(): String{
        if (input.isBlank()) return ""
        return  if (isYoutubeLink(input)){
            input
        }else{
            when(type){
                YoutubeType.URL -> input
                YoutubeType.VIDEO ->"$YOUTUBE_URL$input"
                YoutubeType.CHANNEL -> "$YOUTUBE_CHANNEL$input"
            }
        }
    }

    //获取二维码ID内容
    fun getId(): String {
        // 如果不是链接，说明输入的就是 ID
        if (!isYoutubeLink(input)) return input

        return when (type) {
            // 1. URL 模式：尝试提取关键部分
            YoutubeType.URL -> {
                when {
                    input.contains("watch?v=") -> input.substringAfter("watch?v=").substringBefore("&")
                    input.contains("@") -> "@" + input.substringAfterLast("@").substringBefore("/")
                    else -> input.substringAfterLast("/").substringBefore("?")
                }
            }
            // 2. 视频模式
            YoutubeType.VIDEO -> {
                if (input.contains("watch?v=")) {
                    input.substringAfter("watch?v=").substringBefore("&")
                } else if (input.contains("youtu.be/")) {
                    input.substringAfterLast("/")
                } else {
                    input.substringAfterLast("v=").substringBefore("&")
                }
            }
            // 3. 频道模式
            YoutubeType.CHANNEL -> {
                when {
                    input.contains("@") -> "@" + input.substringAfterLast("@").substringBefore("/")
                    input.contains("/channel/") -> input.substringAfter("/channel/").substringBefore("/")
                    input.contains("/c/") -> input.substringAfter("/c/").substringBefore("/")
                    else -> input.substringAfterLast("youtube.com/").substringBefore("/")
                }
            }
        }.ifBlank { input } // 兜底：如果解析结果为空，返回原始链接
    }
}