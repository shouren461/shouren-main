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
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType
import com.example.shouren.functions.createFunction.YoutubeModel
import com.example.shouren.functions.createFunction.YoutubeType
import com.example.shouren.utils.PictureHelper
import com.example.shouren.utils.QRHelper
import com.google.android.material.textfield.TextInputLayout

class YoutubeCreateActivity: BaseActivity(), View.OnClickListener {
    companion object{
        private const val CATEGORY_URL = 1;
        private const val CATEGORY_VIDEO = 2;
        private const val CATEGORY_CHANNEL = 3;
    }
    //定义延迟初始化控件
    private lateinit var ivBack: ImageView
    private lateinit var tvCreate: TextView
    private lateinit var tvCategoryUrl: TextView
    private lateinit var tvCategoryVideo: TextView
    private lateinit var tvCategoryChannel: TextView
    private lateinit var etInput: EditText
    private lateinit var llMode: LinearLayout
    private lateinit var tilInput: TextInputLayout
    //新增二维码视图按钮  保存按钮  分享按钮
    private lateinit var ivQr: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    private var category: Int =CATEGORY_VIDEO
    //Youtube创建格式模型
    private val youtubeModel = YoutubeModel()
    //Youtube二维码位图
    private var youtubeQRBitMap: Bitmap ?= null
    //定义数据库管理器
    private lateinit var dbManager: HistoryDBManagerHelper
    //获取布局资源
    override fun getLayout(): Int { return R.layout.activity_youtube_create }

    override fun initData() {
        ivBack =findViewById(R.id.ivBack)
        tvCreate = findViewById(R.id.tv_create)
        tvCategoryUrl = findViewById(R.id.tvUrl)
        tvCategoryVideo = findViewById(R.id.tvVideo)
        tvCategoryChannel = findViewById(R.id.tvChannel)
        etInput = findViewById(R.id.et_input)
        llMode =findViewById(R.id.llMode)
        ivQr = findViewById(R.id.iv_qr)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)
        dbManager = HistoryDBManagerHelper(this,1)
        tilInput = findViewById(R.id.til_Input)
    }

    override fun initView() {
        youtubeModel.type = YoutubeType.VIDEO
        tilInput.hint = getString(R.string.hint_youtube_video_id)
        btnSave.visibility = View.GONE
        btnShare.visibility = View.GONE
    }

    override fun initAction() {
        ivBack.setOnClickListener(this);
        tvCreate.setOnClickListener(this)
        tvCategoryUrl.setOnClickListener(this);
        tvCategoryVideo.setOnClickListener(this)
        tvCategoryChannel.setOnClickListener(this)
        etInput.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        btnShare.setOnClickListener(this)
    }

    //创建二维码事件
    private fun createQR(){
        val inputStr = etInput.text?.toString()?.trim() ?:""
        if (inputStr.isEmpty()){
            etInput.error = getString(R.string.error_empty_input)
            return
        }
        youtubeModel.input = inputStr
        youtubeQRBitMap = QRHelper.createQRBitmap(youtubeModel.getContent())
        //跳转到二维码结果展示页
        //生成结果并跳转到结果展示界面 @param isHistory 标记是否查看历史记录
        if (youtubeQRBitMap != null){
            ivQr.setImageBitmap(youtubeQRBitMap)
            btnSave.visibility = View.VISIBLE
            btnShare.visibility = View.VISIBLE
            //保存加载入创建数据表
            val item = HistoryItem(
                title = youtubeModel.input,
                content = youtubeModel.getContent(),
                format = RecordType.YOUTUBE,
                timestamp = System.currentTimeMillis()
            )
            dbManager.insert(HistoryDBManagerHelper.CREATE_TABLE_NAME,item)  //进入创建历史记录页面自动刷新数据
            Toast.makeText(this,getString(R.string.toast_insert_youtube_record_success), Toast.LENGTH_SHORT).show()
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
                tilInput.hint = getString(R.string.hint_youtube_url)
                etInput.text?.clear()
                tvCategoryUrl.setBackgroundResource(R.drawable.bg_mode_selected)
                tvCategoryUrl.setTextColor(getColor(R.color.white))
                tvCategoryVideo.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryVideo.setTextColor(getColor(R.color.black))
                tvCategoryChannel.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryChannel.setTextColor(getColor(R.color.black))
            }
            R.id.tvVideo ->{
                category = CATEGORY_VIDEO
                youtubeModel.type = YoutubeType.VIDEO
                tilInput.hint = getString(R.string.hint_youtube_video_id)
                etInput.text?.clear()
                tvCategoryUrl.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryUrl.setTextColor(getColor(R.color.black))
                tvCategoryVideo.setBackgroundResource(R.drawable.bg_mode_selected)
                tvCategoryVideo.setTextColor(getColor(R.color.white))
                tvCategoryChannel.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryChannel.setTextColor(getColor(R.color.black))
            }
            R.id.tvChannel ->{
                category = CATEGORY_CHANNEL
                youtubeModel.type = YoutubeType.CHANNEL
                tilInput.hint = getString(R.string.hint_youtube_channel_url)
                etInput.text?.clear()
                tvCategoryUrl.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryUrl.setTextColor(getColor(R.color.black))
                tvCategoryVideo.setBackgroundResource(R.drawable.bg_mode_selection)
                tvCategoryVideo.setTextColor(getColor(R.color.black))
                tvCategoryChannel.setBackgroundResource(R.drawable.bg_mode_selected)
                tvCategoryChannel.setTextColor(getColor(R.color.white))
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
