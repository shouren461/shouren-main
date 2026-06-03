package com.example.shouren.utils

//全局定义常量类
object Constant{
    //Intent传递参数:选中主页面的哪个Tab(0:扫描,1:创建,2:历史,3:设置)
    const val EXTRA_SELECT_TAB = "key_select_tab"
}
//SharePreference(SP)存储的Key定义
//Activity跳转请求/结果码
//跳转到创建结果页面的请求码
const val REQUEST_CODE_CREATE_RESULT_PAGE = 1001
//关闭创建输入页面的结果码
const val RESULT_CODE_CLOSE_CREATE_PAGE = 1002