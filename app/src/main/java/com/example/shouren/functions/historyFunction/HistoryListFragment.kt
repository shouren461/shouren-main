package com.example.shouren.functions.historyFunction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.shouren.R
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shouren.functions.historyFunction.HistoryRCVDetail
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem

//历史记录列表Fragment  ->负责展示具体的列表数据(扫描或者创建),处理点击详情，长按进入编辑模式，和批量删除的具体逻辑
class HistoryListFragment: Fragment(){
    //初始化控件成成员变量
    private lateinit var recyclerView: RecyclerView
    //加载数据库管理器
    private val dbManager by lazy {HistoryDBManagerHelper(requireContext(),1)  }
    //从参数中取当前的fragment表名,需要操作的数据库表(默认为扫描表)
    private val tableName by lazy { arguments ?. getString("TABLE_NAME") ?: HistoryDBManagerHelper.SCAN_TABLE_NAME }
    //初始化适配器，并在此定义对 反馈 feedback的处理
    private val  adapter by lazy {
        HistoryListAdapter(emptyList()){ feedback ->
            when(feedback){
                //1,普通模式下点击:跳转详情页 
                is HistoryListAdapter.Feedback.Click ->clickEvent(feedback.item)
                //2,普通模式下长按:进入编辑状态
                is HistoryListAdapter.Feedback.LongPress ->enterEditModel(feedback.item)
                //3,编辑模式下点击，改变选中项状态
                is HistoryListAdapter.Feedback.SelectStatusChanged ->updateSelectStatus(feedback.item)
            }
        }

    }
    private var historyItems = mutableListOf<HistoryItem>()  //本地缓存的数据集合，用于界面展示
    companion object{
        //静态构造方法:方便根据表明创建不同的Fragment实例
        fun newInstance(tableName: String) = HistoryListFragment().apply {
            arguments = Bundle().apply{
                putString("TABLE_NAME",tableName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //1,填充布局文件
        val view  =  inflater.inflate(R.layout.history_list_fragment,container,false)
        //2,绑定控件
        recyclerView = view.findViewById(R.id.historyRCV)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        updateRCVList()
    }
    //每次点击进入历史记录界面回自动刷新
    override fun onResume() {
        super.onResume()
        updateRCVList()
    }
    //更新列表数据:从数据库拉取最新数据，并同步到适配器中刷新UI
     fun updateRCVList() {
       historyItems =  dbManager.getAll(tableName).toMutableList()
        adapter.updateRCVList(historyItems)
    }
    
    private fun initView() {
        //初始化滚动视图
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    //当MainFragment 切换"selectMode->编辑/普通"时,调用RCVList改变选中状态
    fun onModeChanged(currentMode: HistoryMainFragment.PageMode) {
        //1.判断适配器的模式，来决定是否显示复选框
        adapter.isEditMode = (currentMode == HistoryMainFragment.PageMode.EDIT)
        //2,如果退出了编辑模式，重置内存中所有的选中记录
        for (item in historyItems) {
            if (item.isSelected != false){
                item.isSelected = false
            }
        }
        adapter.notifyDataSetChanged()
    }

    //普通模式下点击跳转到详情页面
    private fun clickEvent(item: HistoryItem) {
        HistoryRCVDetail.startActivity(requireContext(),item.id,tableName);
    }
    //普通模式下长按进入编辑状态
    fun enterEditModel(item: HistoryItem) {
        //找到他的父fragment,同时调用父fragment的switchMode()方法，实现状态栏菜单自动进入编辑模式
        (parentFragment as? HistoryMainFragment)?.switchMode(HistoryMainFragment.PageMode.EDIT)
        //onModeChanged(HistoryMainFragment.PageMode.EDIT)   ->这里父类会自动调用onModeChanged()方法实现模式转换和顶部状态栏编辑模式切换
    }
    //编辑模式下点击/长按改变选中项状态
    fun updateSelectStatus(item: HistoryItem) {
        //1,改变选中项
        item.isSelected = !item.isSelected
        //2,刷新所有列表状态
        adapter.notifyDataSetChanged()
    }
    //执行全选或者全去取消操作
    fun selectAll(isAllSelected: Boolean){
        for (item in historyItems) {
            item.isSelected = isAllSelected
        }
        adapter.notifyDataSetChanged()  //通知适配器数据已经发生了改变，刷新UI状态
    }

    //获取当前页面的所有历史记录项
    fun getHistoryItems():List<HistoryItem> {
        return historyItems
    }
    //批量删除列表项
    fun deleteSelectedItem() {
        //1,过滤掉所有的当前被勾选的ids集合
        val selectedList = mutableListOf<Long>()
        for (item in historyItems) {
            if (item.isSelected){
                selectedList.add(item.id)
            }
        }
        //2,调用数据库管理工具批量删除SQL
        dbManager.deleteBatch(tableName,selectedList)
        //3,刷新列表数据
        updateRCVList()
    }


}