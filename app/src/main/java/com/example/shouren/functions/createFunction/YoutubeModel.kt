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
        private fun isYoutubeLink(text: String): Boolean{
            val lowercase = text.lowercase();
            return lowercase.contains("youtube.com")
        }
    }
    //获取完整二维码链接
    fun getContent(): String{
        if (input.isBlank()) return ""
        return  if (isYoutubeLink(input)){
            input
        }else{
            when(type){
                YoutubeType.URL -> input // For URL mode, if it's not a youtube link, just use as is
                YoutubeType.VIDEO ->"$YOUTUBE_URL$input"
                YoutubeType.CHANNEL -> "$YOUTUBE_CHANNEL$input"
            }
        }
    }

    //获取二维码ID内容
    fun getId(): String{
        if (!isYoutubeLink(input)) return input
        return  when(type){
            //1,Url模式
            YoutubeType.URL ->{
                when{
                    input.contains("watch?v=") ->{
                        input.substringAfter("watch?v=")
                            .substringBefore("&")
                    }
                    input.contains("@")->{
                        input.substringAfter("/www.youtube.com/")
                    }
                    else ->""
                }
            }
            //2,视频模式
            YoutubeType.VIDEO ->{
                    if (input.contains("watch?v=")){
                        input.substringAfter("/www.youtube.com/")
                    }else{
                        ""
                    }
            }
            //3,频道模式
            YoutubeType.CHANNEL ->{
                if (input.contains("@")){
                    input.substringAfter("/www.youtube.com")
                }else{
                    ""
                }
            }
        }
    }

}