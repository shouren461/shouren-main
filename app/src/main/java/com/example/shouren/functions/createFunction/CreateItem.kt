package com.example.shouren.functions.createFunction

import com.example.shouren.R

//创建二维码的枚举类型  ->关联了每种类型的显示文本资源Id 和 对应的图标资源Id
enum class CreateItemType (val stringSrc : String ,val typeIcon : Int){
    YOUTUBE("youtube", R.drawable.vector_ic_youtube),
    CALENDAR("日历",R.drawable.vector_ic_calendar)
}

class CreateItem(val name: String ,val type : CreateItemType){

}
