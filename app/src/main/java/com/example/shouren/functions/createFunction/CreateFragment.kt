package com.example.shouren.functions.createFunction

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shouren.activity.BaseFragment
import com.example.shouren.R
import com.example.shouren.activity.CalendarCreateActivity
import com.example.shouren.activity.YoutubeCreateActivity

class CreateFragment: BaseFragment(), CreateItemClickListener{

    //1，获取布局资源
    override fun getLayoutResId(): Int   = R.layout.fragment_create
    //2,初始化数据
    override fun initData() {
        //目前不需要初始化数据
    }
    //3,初始化视图
    override fun initView(root: View?) {
        val recyclerView = root?.findViewById<RecyclerView>(R.id.rcv_create)
        //为recyclerView 滚动视图设置 布局管理器(layoutManager) 和 适配器(adapter)
           //注意requireContext()是宿主线程上下文   requireActivity()是宿主Activity上下文
        recyclerView?.layoutManager = GridLayoutManager(requireContext(),2)
        val adapter = CreateRCVAdapter(requireActivity(), this)
        recyclerView?.adapter = adapter;

        //重新加载数据
        adapter.reloadData(CreateEntity.typeList)
    }

    //4,初始化点击器  ->点击跳转到对应界面
    override fun onCreateItemClickListener(
        position: Int,
        item: CreateItem
    ) {
        when(item.type){
            CreateItemType.YOUTUBE -> {
                val  intent = Intent(activity, YoutubeCreateActivity::class.java)
                startActivity(intent)
            }
            CreateItemType.CALENDAR -> {
                val intent = Intent(activity, CalendarCreateActivity::class.java)
                startActivity(intent)
            }
        }
    }

}