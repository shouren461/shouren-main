package com.example.shouren.activity

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.shouren.R
import com.example.shouren.functions.createFunction.YoutubeModel
import com.example.shouren.functions.createFunction.YoutubeType
import com.example.shouren.utils.PictureHelper
import com.example.shouren.utils.QRHelper

class YoutubeCreateActivity: BaseActivity(), View.OnClickListener {
    companion object{
        private const val CATEGORY_URL = 1;
        private const val CATEGORY_VIDEO = 2;
        private const val CATEGORY_CHANNEL = 3;
    }
    //定义延迟初始化控件
    private lateinit var iv_back: ImageView
    private lateinit var tv_create: TextView
    private lateinit var tv_category_url: TextView
    private lateinit var tv_category_video: TextView
    private lateinit var tv_category_channel: TextView
    private lateinit var et_input: EditText
    private lateinit var ll_mode: LinearLayout
    //新增二维码视图按钮  保存按钮  分享按钮
    private lateinit var iv_qr: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    private var category: Int =CATEGORY_VIDEO
    //Youtube创建格式模型
    private val youtubeModel = YoutubeModel()
    //Youtube二维码位图
    private var youtubeQRBitMap: Bitmap ?= null
    //获取布局资源
    override fun getLayout(): Int { return R.layout.activity_youtube_create }

    override fun initData() {
        iv_back =findViewById(R.id.ivBack)
        tv_create = findViewById(R.id.tv_create)
        tv_category_url = findViewById(R.id.tvUrl)
        tv_category_video = findViewById(R.id.tvVideo)
        tv_category_channel = findViewById(R.id.tvChannel)
        et_input = findViewById(R.id.et_input)
        ll_mode =findViewById(R.id.llMode)
        iv_qr = findViewById(R.id.iv_qr)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)
    }

    override fun initView() {
        btnSave.visibility = View.GONE
        btnShare.visibility = View.GONE
    }

    override fun initAction() {
        iv_back.setOnClickListener(this);
        tv_create.setOnClickListener(this)
        tv_category_url.setOnClickListener(this);
        tv_category_video.setOnClickListener(this)
        tv_category_channel.setOnClickListener(this)
        et_input.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        btnShare.setOnClickListener(this)
    }

    //创建二维码事件
    private fun createQR(){
        val inputStr = et_input.text?.toString()?.trim() ?:""
        if (inputStr.isEmpty()){
            et_input.error = getString(R.string.error_empty_input)
            return
        }
        youtubeModel.input = inputStr
        youtubeQRBitMap = QRHelper.createQRBitmap(youtubeModel.getContent())
        //跳转到二维码结果展示页
        //生成结果并跳转到结果展示界面 @param isHistory 标记是否查看历史记录
        if (youtubeQRBitMap != null){
            iv_qr.setImageBitmap(youtubeQRBitMap)
            btnSave.visibility = View.VISIBLE
            btnShare.visibility = View.VISIBLE
        }
    }
    //点击事件
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ivBack ->finish()
            R.id.tv_create ->createQR()
            R.id.tvUrl -> {
                category = CATEGORY_URL
                youtubeModel.type = YoutubeType.URL
                et_input.hint = getString(R.string.hint_youtube_url)
                tv_category_url.setBackgroundResource(R.drawable.bg_mode_selected)
                tv_category_url.setTextColor(getColor(R.color.white))
                tv_category_video.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_video.setTextColor(getColor(R.color.black))
                tv_category_channel.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_channel.setTextColor(getColor(R.color.black))
            }
            R.id.tvVideo ->{
                category = CATEGORY_VIDEO
                youtubeModel.type = YoutubeType.VIDEO
                et_input.hint = getString(R.string.hint_youtube_video_id)
                tv_category_url.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_url.setTextColor(getColor(R.color.black))
                tv_category_video.setBackgroundResource(R.drawable.bg_mode_selected)
                tv_category_video.setTextColor(getColor(R.color.white))
                tv_category_channel.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_channel.setTextColor(getColor(R.color.black))
            }
            R.id.tvChannel ->{
                category = CATEGORY_CHANNEL
                youtubeModel.type = YoutubeType.CHANNEL
                et_input.hint = getString(R.string.hint_youtube_channel_url)
                tv_category_url.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_url.setTextColor(getColor(R.color.black))
                tv_category_video.setBackgroundResource(R.drawable.bg_mode_selection)
                tv_category_video.setTextColor(getColor(R.color.black))
                tv_category_channel.setBackgroundResource(R.drawable.bg_mode_selected)
                tv_category_channel.setTextColor(getColor(R.color.white))
            }
            //保存按钮
            R.id.btnSave ->{
                youtubeQRBitMap?.let { saveQR(it) }
            }
            //分享按钮
            R.id.btnShare ->{
                shareQR()
            }
        }
    }

    //保存图片到相册中
    private fun saveQR(bitmap: Bitmap){
        when(val  result  = PictureHelper.savePicture(this,bitmap)){
            is PictureHelper.SaveInfo.Success -> {
                Toast.makeText(this,getString(R.string.saved_picture), Toast.LENGTH_SHORT).show()
            }
            is PictureHelper.SaveInfo.Error ->{
                Toast.makeText(this,getString(R.string.save_failed, result.message), Toast.LENGTH_SHORT).show()
            }
            //请求权限失败
            is PictureHelper.SaveInfo.PermissionRequired -> {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    //分享图片
    private fun shareQR(){
        youtubeQRBitMap?.let { bitmap ->
            PictureHelper.shareWith(this, bitmap)?.let { intent ->
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
            } ?: Toast.makeText(this, getString(R.string.error_save_unknown), Toast.LENGTH_SHORT).show()
        }
    }
}
