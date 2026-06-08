package com.example.shouren.functions.historyFunction

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.shouren.R
import com.example.shouren.activity.BaseActivity
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType
import com.example.shouren.utils.PictureHelper
import com.example.shouren.utils.QRHelper

class HistoryRCVDetail: BaseActivity(), View.OnClickListener {

    //定义延迟初始化控件
    private  lateinit var ivBack: ImageView
    private  lateinit var ivIcon: ImageView
    private  lateinit var tvType: TextView
    private  lateinit var tvTitle: TextView
    private  lateinit var tvContent: TextView
    private  lateinit var ivQR: ImageView
    private  lateinit var btnSave: Button
    private  lateinit var btnShare: Button

    private  lateinit var dbManager: HistoryDBManagerHelper
    private var currentItem: HistoryItem ?=null   //定义当前选中项
    private  var currentQRBitmap: Bitmap ?= null     //定义当前二维码视图
    companion object {
        const val HISTORY_ITEM_ID = "item_id"
        const val TABLE_NAME = "tableName"
        fun startActivity(context: Context, id: Long, tableName: String) {
            val intent = Intent(context, HistoryRCVDetail::class.java)
            intent.putExtra(HISTORY_ITEM_ID,id)
            intent.putExtra(TABLE_NAME,tableName)
            context.startActivity(intent)
        }
    }

    override fun getLayout(): Int  = R.layout.history_item_detail

    override fun initData() {
      dbManager = HistoryDBManagerHelper(this,1)
    }

    override fun initView() {
        ivBack = findViewById(R.id.ivBack)
        ivIcon = findViewById(R.id.ivIcon)
        tvType = findViewById(R.id.tvType)
        tvTitle = findViewById(R.id.tvTitle)
        tvContent = findViewById(R.id.tvContent)
        ivQR = findViewById(R.id.ivQR)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)

        //使用post确保在下一帧执行，此时视图肯定已经完全布局且变量已赋值
        window.decorView.post {
            initHistoryItemList()
        }
    }
    //绑定监听事件
    override fun initAction() {
        ivBack.setOnClickListener(this)
        btnSave.setOnClickListener(this)
        btnShare.setOnClickListener(this)
    }
    //初始化历史视图列表
    private fun initHistoryItemList() {
        val itemId = intent.getLongExtra(HISTORY_ITEM_ID,-1L)
        val tableName = intent.getStringExtra(TABLE_NAME) ?: HistoryDBManagerHelper.SCAN_TABLE_NAME
        
        currentItem = dbManager.selectById(itemId, tableName)
        
        if (currentItem != null){
            showItemListDetail(currentItem)
        } else {
            Toast.makeText(this,getString(R.string.error_record_not_found), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    //展示列表信息，包括图标，图标类型，标题，二维码
    private fun showItemListDetail(item: HistoryItem?) {
        if (item != null) {
            tvTitle.text = item.title
            tvContent.text = item.content
        }
        //生成标题和图标
        if (item != null) {
           val iconId =  when(item.format) {
                RecordType.YOUTUBE -> R.drawable.vector_ic_youtube
                RecordType.CALENDAR -> R.drawable.vector_ic_calendar
                RecordType.TEXT -> R.drawable.vector_ic_text
            }
            val tvTypeId =  when(item.format) {
                RecordType.YOUTUBE -> R.string.type_youtube
                RecordType.CALENDAR -> R.string.type_calendar
                RecordType.TEXT -> R.string.type_text
            }
            ivIcon.setImageResource(iconId)
            tvType.setText(tvTypeId)
        }
        //生成二维码
        if (item != null) {
            currentQRBitmap = QRHelper.createQRBitmap(item.content)
            ivQR.setImageBitmap(currentQRBitmap)
        }
    }

    //监听点击事件
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ivBack -> finish()
            R.id.btnSave -> savePicture()
            R.id.btnShare -> sharePicture()
        }
    }

    //分享图片
    private fun sharePicture() {
        currentQRBitmap?.let { bitmap ->
            val intent = PictureHelper.shareWith(this, bitmap) ?: return
            startActivity(Intent.createChooser(intent,getString(R.string.share)))
        } ?: run{
            Toast.makeText(this,getString(R.string.error_qr_image_not_found), Toast.LENGTH_SHORT).show()
        }
    }
    //保存图片
    private fun savePicture() {
        currentQRBitmap?.let { bitmap ->
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
    }

}