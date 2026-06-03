package com.example.shouren.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

//所有Fragment的基类，，规范了生命周期和视图初始化
abstract class BaseFragment: Fragment(){
    //创建基础视图
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(getLayoutResId(), container, false);
        initView(root);
        initData();
        return root;
    }


    //初始化布局资源
    abstract fun getLayoutResId(): Int;
    abstract fun initData();
    abstract fun initView(root: View?);


}