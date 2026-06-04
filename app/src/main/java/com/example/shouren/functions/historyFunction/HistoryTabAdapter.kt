package com.example.shouren.functions.historyFunction

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.shouren.database.HistoryDBManagerHelper

//历史页面Tab适配器 ->负责子啊ViewPage2中切换 "创建历史" 和 "扫描历史"
class HistoryTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment){

    //根据位置创建对应的Fragment positon0:扫描历史   positon1:创建历史
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 ->{
                //创建一个显示 扫描表的fragment
                HistoryListFragment.newInstance(HistoryDBManagerHelper.SCAN_TABLE_NAME)
            }
            else -> {
                //创建一个显示 创建表的fragment
                HistoryListFragment.newInstance(HistoryDBManagerHelper.CREATE_TABLE_NAME)
            }
        }
    }

    //页面数量固定为2 -> 扫描历史 和 创建历史
    override fun getItemCount(): Int  = 2
}