package com.example.shouren.functions.createFunction

class CreateEntity {
    companion object{
        val typeList: List<CreateItem> = listOf(
            CreateItem("YouTube", CreateItemType.YOUTUBE),
            CreateItem("日历", CreateItemType.CALENDAR)
        )
    }

    //将本项目定义的CreateType转化为底层库需要的CreateFormat类型
 /*   fun transCreateWithEntity(type: CreateItemType):CreateFormat{
        return when(type){
            CreateItemType.CALENDAR ->CreateFormat.Calendar
            CreateItemType.YOUTUBE -> CreateFormat.Youtube
        }
    }*/
}