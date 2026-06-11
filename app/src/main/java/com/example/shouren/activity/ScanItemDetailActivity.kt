package com.example.shouren.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.shouren.R
import com.example.shouren.database.RecordType
import com.example.shouren.functions.createFunction.CalendarModel
import java.time.format.DateTimeFormatter
import java.util.Calendar

//扫码详情页 ->作为结果展示的"容器",并根据不同的二码类型提供快捷操作
class ScanItemDetailActivity: AppCompatActivity() {
    //定义延迟初始化控件
    private lateinit var ivIconType: ImageView
    private lateinit var tvTypeLabel: TextView
    private lateinit var tvContent: TextView
    private lateinit var btnAction: View
    private lateinit var btnShare: View
    //定义初始数据和二维码数据类型
    private var initResult: String = ""
    private var resultType: RecordType = RecordType.TEXT
    companion object{
        private const val EXTRA_RESULT  = "result"
        private const val EXTRA_TYPE = "type"
        //静态启动方法:其他页面调用时必须传入结果和类型
        fun startActivity(context: Context, result: String, type: RecordType){
            val intent = Intent(context, ScanItemDetailActivity::class.java)
            intent.putExtra(EXTRA_RESULT,result)
            intent.putExtra(EXTRA_TYPE,type)
            context.startActivity(intent)
        }
    }
    //创建Activity生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_item_detail)
        //1,获取intent传递过来的原始文本和二维码类型
        initResult = intent.getStringExtra(EXTRA_RESULT) ?: ""
        resultType = intent.getSerializableExtra(EXTRA_TYPE) as? RecordType ?: RecordType.TEXT
        //2,调用控件绑定方法
        initViews()
        //3,将数据渲染到页面上
        showScanResultInfo()
    }
    //初始化视图 ->绑定视图控件
    private fun initViews() {
       val toolbar = findViewById<Toolbar>(R.id.toolbarZxing)
        setSupportActionBar(toolbar)
        //设置返回箭头的点击事件
        toolbar.setNavigationOnClickListener { finish() }

        ivIconType = findViewById(R.id.ivIconTypeZxing)
        tvTypeLabel = findViewById(R.id.tvTypeLabelZxing)
        tvContent = findViewById(R.id.tvContentZxing)
        btnAction = findViewById(R.id.btnActionZxing)
        btnShare  =findViewById(R.id.btnShareZxing)
        //全局分享按钮
        btnShare.setOnClickListener{shareResult()}
    }

    //将数据渲染到页面上,根据内容动态切换UI表现
    private fun showScanResultInfo() {
        //关于详情页的图标和图标类型要根据二维码类型来来判断
        when(resultType){
            RecordType.YOUTUBE -> {
                tvContent.text = initResult
                ivIconType.setImageResource(R.drawable.vector_ic_youtube)
                tvTypeLabel.text = getString(R.string.type_youtube)
                btnAction.setOnClickListener { jumpToWebsite(initResult) }
            }
              RecordType.CALENDAR -> {
                ivIconType.setImageResource(R.drawable.vector_ic_calendar)
                tvTypeLabel.text = getString(R.string.type_calendar)

                // 解析并显示可读的日程摘要
                val model = CalendarModel.Companion.fromString(initResult)
                if (model != null) {
                    val summary = StringBuilder()
                    summary.append("${model.title}\n")
                    if (model.location.isNotBlank()) {
                        summary.append("地点: ${model.location}\n")
                    }
                    summary.append("时间: ${formatDateTime(model)}")
                    tvContent.text = summary.toString()
                } else {
                    tvContent.text = initResult
                }

                btnAction.setOnClickListener { addSchedule() }
                (btnAction as? TextView) ?.text = getString(R.string.add_to_calendar)
            }
            else -> {
                tvContent.text = initResult
                ivIconType.setImageResource(R.drawable.vector_ic_text)
                tvTypeLabel.text = getString(R.string.type_text)
                btnAction.setOnClickListener { shareResult() }
                (btnAction as? TextView) ?.text = getString(R.string.share)
            }
        }
    }

    // 辅助方法：格式化日程时间范围
    private fun formatDateTime(model: CalendarModel): String {
        val formatter = if (model.isAllDay)
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        else
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        return "${model.startTime.format(formatter)} - ${model.endTime.format(formatter)}"
    }

    //跳转到指定网页操作
    fun jumpToWebsite(url: String) {
        try {
            //Intent.ACTION_VIEW是通用的查看协议
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }catch (e: Exception){
            Toast.makeText(this,getString(R.string.error_link_is_not_clickable), Toast.LENGTH_SHORT).show()
        }
    }
    //新建日程操作 ->将解析出的iCalendar数据写入手机系统日历
    fun addSchedule() {
        //调用日历模型的解析器
        val calendarModel = CalendarModel.Companion.fromString(initResult) ?: return
        //创建一个系统日历的插入意图
        val insertIntent = Intent(Intent.ACTION_INSERT).apply {
            setDataAndType(CalendarContract.CONTENT_URI, "vnd.android.cursor.dir/event")  //setDataAndType()精确的勾起日历应用响应
            putExtra(CalendarContract.Events.TITLE,calendarModel.title)
            putExtra(CalendarContract.Events.DESCRIPTION,calendarModel.description)
            putExtra(CalendarContract.Events.EVENT_LOCATION,calendarModel.location)

            calendarModel.startTime?.let {
                val calendar = Calendar.getInstance()
                calendar.set(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.timeInMillis)
            }
            // 同时设置结束时间
            calendarModel.endTime?.let {
                val calendar = Calendar.getInstance()
                calendar.set(it.year, it.monthValue - 1, it.dayOfMonth, it.hour, it.minute)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendar.timeInMillis)
            }
        }
        try {
            startActivity(insertIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "未找到日历应用", Toast.LENGTH_SHORT).show()
        }
    }
    //分享二维码的详情页文本内容结果给其他应用
    fun shareResult() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,initResult)
        }
        //使用Chooser来强制让用户选择一个应用进行分享
        startActivity(Intent.createChooser(intent,getString(R.string.share)))
    }


}