package com.example.shouren.functions.historyFunction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.example.shouren.R
import androidx.recyclerview.widget.RecyclerView
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType

class HistoryListAdapter(
    private var list: List<HistoryItem>,       //数据源
    private val onFeedback: (Feedback) -> Unit  //用户交互的 回调(点击，长按)
) : RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {
    //定义一个内部类ViewHolder 绑定布局中的控件
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: AppCompatImageView = view.findViewById(R.id.ivIcon)
        val ivArrow: AppCompatImageView = view.findViewById(R.id.ivArrow)
        var tvTitle: TextView = view.findViewById(R.id.tvTitle)
        var tvType: TextView = view.findViewById(R.id.tvType)
    }

    //定义用户交互的密封类 ->封装点击，长按,和勾选状态改变的三种行为
    sealed class Feedback {
        data class Click(val item: HistoryItem) : Feedback()     //普通模式下的点击
        data class LongPress(val item: HistoryItem) : Feedback()  //普通模式下的长按
        data class SelectStatusChanged(val item: HistoryItem) : Feedback()   //普编辑模式下的勾选/取消
    }
    //判断是否处于编辑模式
    var isEditMode: Boolean = false
        set(value){
            field = value
            notifyDataSetChanged()  //模式切换时刷新整个列表
        }
    //更新列表数据的方法
    fun updateRCVList(newList:List<HistoryItem>){
        this.list = newList
        notifyDataSetChanged()     //刷新列表数据
    }

//创建视图控件
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryListAdapter.ViewHolder {
        //加载单条历史的布局文件
    val view =
        LayoutInflater.from(parent.context).inflate(R.layout.history_rcv_item, parent, false)
    return ViewHolder(view)
}

    //绑定视图控价
    override fun onBindViewHolder(viewHolder: HistoryListAdapter.ViewHolder, position: Int) {
        val historyItem = list[position]

        //1,设置基本文本信息
        viewHolder.tvTitle.text = historyItem.title
        viewHolder.tvType.text = historyItem.format.toString()

        //2,设置左侧类型图标(根据不同的二维码类型显示不同的图片)
        val iconID = when(historyItem.format){
            RecordType.YOUTUBE -> R.drawable.vector_ic_youtube
            RecordType.CALENDAR ->R.drawable.vector_ic_calendar
            else ->  R.drawable.vector_ic_text
        }
        viewHolder.ivIcon.setImageResource(iconID)

        //3,处理右侧状态图标  ->如果是普通模式，显示默认箭头图标  ；如果是编辑模式 ->显示勾选框(已选或者未选)
        if (isEditMode){
            val checkBoxId:Int
            if (historyItem.isSelected){
                checkBoxId =  R.drawable.ic_historyitem_selected
            }else{
                checkBoxId = R.drawable.ic_historyitem_unselected
            }
            viewHolder.ivArrow.setImageResource(checkBoxId)
        }else{
            viewHolder.ivArrow.setImageResource(R.drawable.ic_arrow_detail)
        }

        //3处理点击事件:根据当前模式决定是跳转详情还是选中状态
        viewHolder.itemView.setOnClickListener{
            //3.1 如果是编辑模式，改变选中状态
            if (isEditMode){
                onFeedback(Feedback.SelectStatusChanged(historyItem))
            }else{
                //3.2 如果是普通模式，通知fragment跳转详情页
                onFeedback(Feedback.Click(historyItem))
            }
        }
        //4,处理长按事件 ->仅在普通模式下长按进入编辑模式
        viewHolder.itemView.setOnLongClickListener {
            if (!isEditMode){
                onFeedback(Feedback.LongPress(historyItem))
            }
            true
        }
        //5,

    }

    //获取列表项数量
    override fun getItemCount(): Int  =list.size


}