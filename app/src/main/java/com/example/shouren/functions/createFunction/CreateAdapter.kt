package com.example.shouren.functions.createFunction

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shouren.R

//创建列表的适配器  ->负责将不同的创建类型(CreateItem)渲染到列表项中
class CreateRCVAdapter(
    private val mActivity: Activity,  //上下文对象
    private val createItemClickListener: CreateItemClickListener
    ): RecyclerView.Adapter<CreateRCVAdapter.CreateViewHolder>() {
        //定义一个视图内部类，用于管理视图控件信息
    inner class CreateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val createImg: ImageView = itemView.findViewById(R.id.createImg)
        val createName: TextView = itemView.findViewById(R.id.createName)
    }
    private val  createLayoutInflate: LayoutInflater = LayoutInflater.from(mActivity) //初始化布局加载器
    private var createItemList: List<CreateItem> = ArrayList(1)       //初始化列表集合

        //1,创建适配器视图
        override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        position: Int
    ): CreateRCVAdapter.CreateViewHolder {
        return CreateViewHolder(createLayoutInflate.inflate(R.layout.create_rcv_item,viewGroup,false));
    }
    //绑定适配器视图  -> 根据点击位置获取列表集合
    override fun onBindViewHolder(
        viewHolder: CreateViewHolder,
        position: Int
    ) {
        val createItem = createItemList[position]
        viewHolder.itemView.setOnClickListener{
           createItemClickListener.onCreateItemClickListener(viewHolder.adapterPosition,createItem);
        }
        //2,为viewHolder赋初值
        viewHolder.createName.text = createItem.name
        when(createItem.type){
            CreateItemType.YOUTUBE ->{
                viewHolder.createImg.setImageResource(R.drawable.vector_ic_youtube)
            }
            CreateItemType.CALENDAR ->{
                viewHolder.createImg.setImageResource(R.drawable.vector_ic_calendar);
            }
        }

    }

    //3,获取列表集合长度
    override fun getItemCount(): Int {
        return createItemList.size;
    }

    //4,更新数据源
    fun reloadData(list: List<CreateItem>){
        createItemList = list;
        //通知数据发生变化
        notifyDataSetChanged()
    }


}