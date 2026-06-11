package com.example.shouren

import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentTransaction
import com.example.shouren.activity.BaseActivity
import com.example.shouren.functions.createFunction.CreateFragment
import com.example.shouren.functions.historyFunction.HistoryMainFragment
import com.example.shouren.functions.scanFunction.ScanFragment
import com.example.shouren.utils.Constant

//应用的主界面Activity
// 采用单Activity和 多Fragment 的格式 负责管路底部导航栏和 "扫描"，"创建"，"历史记录"三个核心功能页面的切换
class MainActivity : BaseActivity() {
    //延迟初始化底部标签
    //定义底部的Tab的类型常量
    companion object{
       const val TAB_TYPE_SCAN: Int = 1 //扫描二维码Tab
       const val TAB_TYPE_CREATE: Int = 2 //创建二维码Tab
       const val TAB_TYPE_HISTORY: Int = 3 //历史记录Tab
    }

    //初始化布局控件
    //底部导航栏的UI控件引用(图标)
    private lateinit var scanTab: ImageView
    private lateinit var historyTab: ImageView
    private lateinit var createTab: ImageView

    //记录当前选中的Tab类型，默认显示"创建"页
    var cursorTab = TAB_TYPE_CREATE

    //初始化Fragment资源
    private var createFragment: CreateFragment? = null
    private var historyMainFragment: HistoryMainFragment? = null
    private var scanFragment: ScanFragment? = null
    //返回布局资源
    override fun getLayout(): Int { return R.layout.activity_main; }

    override fun initData() {
        //初始化viewModel ,用于处理Activity的数据逻辑

    }

    override fun initView() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //初始化视图控件
        initBottomTab();
        //从Intent中获取选中的Tab ,如果没有则选择初始值
        val intent = intent
        if(intent != null){
            val selectTab = intent.getIntExtra(Constant.EXTRA_SELECT_TAB,cursorTab);
            when(selectTab){
                1 ->cursorTab = TAB_TYPE_SCAN
                2 ->cursorTab = TAB_TYPE_CREATE
                3 ->cursorTab = TAB_TYPE_HISTORY
            }
        }
        //切换页面
        switchFragment(cursorTab);
    }

    override fun initAction() {}

    //初始化底部导航栏事件
        //初始化底部导航栏
    private fun initBottomTab() {
            scanTab = findViewById(R.id.iv_bottom_scan)
            historyTab = findViewById(R.id.iv_bottom_history)
            createTab = findViewById(R.id.iv_bottom_create)

            //扫描功能入口，目前处在尚未开放状态
            findViewById<View>(R.id.bottom_scan).setOnClickListener {
                onBottomTabSelect(TAB_TYPE_SCAN)
            }
            //历史Tab,点击监听
            findViewById<View>(R.id.bottom_history).setOnClickListener {
                onBottomTabSelect(TAB_TYPE_HISTORY)
            }

            //创建Tab,点击监听
            findViewById<View>(R.id.bottom_create).setOnClickListener {
                onBottomTabSelect(TAB_TYPE_CREATE)

            }
            //初始化底栏状态:设置默认选中状态"创建"图标
            scanTab.setImageResource(R.drawable.vector_ic_tab_scan_unselected)
            historyTab.setImageResource(R.drawable.vector_ic_tab_history_unselected)
            createTab.setImageResource(R.drawable.vector_ic_tab_creat_unselected)
        }

    //处理底部Tab选中的UI更新逻辑
    fun onBottomTabSelect(type: Int) {
        if (type == cursorTab) {
            return   //如果是当前选中的Tab，直接返回
        } else {
            switchFragment(type) //否则切换Fragment
        }
    }

    //切换页面
    //核心逻辑:切换Fragment  ->使用hide/show 方式，避免Fragment 重复创建并保持页面状态
    private fun switchFragment(type: Int) {
        cursorTab = type
            val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            //1,检查Fragment是否已经存在(可能是SaveInstanceState恢复的)
        if (scanFragment == null) {
            val mayExistFragment = supportFragmentManager.findFragmentByTag("f1")
            if (mayExistFragment is ScanFragment) {
                scanFragment = mayExistFragment
            }
        }
        if (historyMainFragment == null) {
            val maybeExistFragment = supportFragmentManager.findFragmentByTag("f2")
            if (maybeExistFragment is HistoryMainFragment) {
                historyMainFragment = maybeExistFragment
            }
        }
        if (createFragment == null) {
            val mayExistFragment = supportFragmentManager.findFragmentByTag("f3")
            if (mayExistFragment is CreateFragment) {
                createFragment = mayExistFragment
            }
        }

            //2,先隐藏所有已存在的Fragment
        historyMainFragment?.let { fragmentTransaction.hide(it) }
        createFragment?.let { fragmentTransaction.hide(it) }
        scanFragment?.let { fragmentTransaction.hide(it) }
            //3,根据点击的类型显示对应的Fragment
            when (type) {
                TAB_TYPE_SCAN ->{
                    if (scanFragment == null){
                        scanFragment = ScanFragment()
                        fragmentTransaction.add(R.id.fl_fragment_holder,scanFragment!!,"f1")
                    }else{
                        fragmentTransaction.show(scanFragment!!)
                    }
                }
                TAB_TYPE_CREATE  -> {
                    if (createFragment == null) {
                        createFragment = CreateFragment()
                        fragmentTransaction.add(R.id.fl_fragment_holder, createFragment!!, "f2")
                    } else {
                        fragmentTransaction.show(createFragment!!)
                    }
                }

                TAB_TYPE_HISTORY -> {
                    if (historyMainFragment == null) {
                        historyMainFragment = HistoryMainFragment()
                        fragmentTransaction.add(R.id.fl_fragment_holder, historyMainFragment!!, "f3")
                    } else {
                        fragmentTransaction.show(historyMainFragment!!)
                    }
                }


            }
            //4,提交事务，允许状态丢失(防止极端情况下的崩溃)
            fragmentTransaction.commitAllowingStateLoss()
        }
}