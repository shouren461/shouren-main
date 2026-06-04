package com.example.shouren.functions.historyFunction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.shouren.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

//历史界面主入口fragment  -> 管理Tab切换和"编辑/普通模式的切换"
class HistoryMainFragment: Fragment() {
     //页面模式定义:Normal(查看)  Edit(编辑)
    enum class PageMode {NORMAL,EDIT}
    //当前默认选择模式是Normal模式
    private var currentMode = PageMode.NORMAL

    //视图控件成员变量
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayOut: TabLayout
    private lateinit var viewPage2 : ViewPager2

    private val tabAdapter by lazy { HistoryTabAdapter(this) }

    //初始化视图控件
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //1,手动填充布局文件
        val view = inflater.inflate(R.layout.fragment_history,container,false)
        //2,手动绑定xml文件中的控件
        toolbar = view.findViewById(R.id.toolbar)
        tabLayOut  = view.findViewById(R.id.tabLayout)
        viewPage2 = view.findViewById(R.id.pager)

        return view
    }
    //初始化视图
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    //初始化视图组件
    private fun initView() {
        viewPage2.adapter = tabAdapter

        //绑定TabLayout 和ViewPager2,设置两个Tab的标题文字
        TabLayoutMediator(tabLayOut,viewPage2){tab,position ->
            tab.text =
                if (position == 0)
            {getString(R.string.tab_scan)}
            else
            { getString(R.string.tab_create)}

            setupTooBar()
            //扫描与创建历史tab相互切换时，自动重置为normal模式
            viewPage2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    switchMode(PageMode.NORMAL)
                }
            })
        }
    }
    //配置ToolBar使其支持顶部菜单
    private fun setupTooBar() {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)   //告诉系统此Fragment有自定义菜单
    }
    //切换模式 ->切换到指定的模式
    private fun switchMode(mode: PageMode) {
        currentMode = mode
        //1,通知系统刷新顶部菜单(onCreateOptionsMenu回被重新调用)
        activity ?.invalidateOptionsMenu()
        //2,通知当前显示的子Fragment :模式改变了，请刷新列表UI
        notifyPagerFragmentModeChange()
    }
    //跨层通讯 -> 通过ViewPage2.currentItem规则，找到正在显示的字Fragment，并调用onModeChanged方法同步更新状态
    private fun notifyPagerFragmentModeChange() {
        //"f" + position 是ViewPage2 默认给Fragment 设置的tag规则
        val currentFragment = childFragmentManager.findFragmentByTag("f"+ viewPage2.currentItem) as? HistoryListFragment
        currentFragment?.onModeChanged(currentMode)
    }

    //根据当前模式加载不同的菜单布局
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (currentMode == PageMode.NORMAL){
            inflater.inflate(R.menu.history_menu_normal,menu)  //普通模式:进显示进入删除图标
        }else{
            inflater.inflate(R.menu.history_menu_edit,menu)    //编辑模式: 显示取消和确定删除
        }
    }

    //处理顶部菜单栏的点击事件
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.delete_mode -> switchMode(PageMode.NORMAL)  //点击垃圾桶图标进入编辑模式
            R.id.cancel ->switchMode(PageMode.NORMAL)        //编辑模式下点击取消图标返回普通模式
            R.id.delete_confirm -> deleteConfirm()            //用户勾选菜单项  ->执行删除
        }
        return super.onOptionsItemSelected(item)
    }
    //
    private fun deleteConfirm() {
       val currentFragment = childFragmentManager.findFragmentByTag("f"+viewPage2.currentItem) as? HistoryListFragment
        currentFragment?.deleteSelectedItem()    //调用子Fragment删除选中的数据项
        switchMode(PageMode.NORMAL)   //删除完之后切换为普通模式
    }


}