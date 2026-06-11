package com.example.shouren.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.example.shouren.R
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType
import com.example.shouren.functions.createFunction.CalendarModel
import com.example.shouren.utils.PictureHelper
import com.example.shouren.utils.QRHelper
import java.time.LocalDateTime

class CalendarCreateActivity: BaseActivity(), View.OnClickListener,  CompoundButton.OnCheckedChangeListener {
    //1,延迟初始化控件
    private lateinit var ivBack: ImageView
    private lateinit var tvCreate: TextView
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchAllday: Switch
    private  var isAllDay = false
    private lateinit var calendarStart: Calendar
    private lateinit var calendarEnd: Calendar
    private lateinit var calendarTemp: Calendar
    private lateinit var ivQr: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    //引入日历模型
    private val calendarModel = CalendarModel()
    //引入二维码位图
    private var calendarQRBitmap: Bitmap ?= null
    //定义二维码数据库工具类
    private lateinit var dbManager: HistoryDBManagerHelper

    companion object {
        private const val DATE_START = 1
        private const val DATE_END = 2
    }
    //初始化布局
    override fun getLayout(): Int = R.layout.activity_calendar_create
    //初始化数据逻辑
    @SuppressLint("CutPasteId")
    override fun initData() {
        ivBack = findViewById(R.id.iv_back)
        tvCreate = findViewById(R.id.tv_create)
        etTitle = findViewById(R.id.etTitle)
        etLocation = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        switchAllday = findViewById(R.id.switchAllDay) // Added missing initialization
        ivQr = findViewById(R.id.iv_qr)
        btnSave = findViewById(R.id.btnSave)
        btnShare =findViewById(R.id.btnShare)

        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance()
        calendarEnd.add(Calendar.HOUR_OF_DAY,1)
        calendarTemp = Calendar.getInstance()
        dbManager = HistoryDBManagerHelper(this,1)
    }

    //初始化视图
    override fun initView() {
        initDateTime()    //初始化时间
        updateDateTime()   //更新时间

        btnSave.visibility = View.GONE
        btnShare.visibility = View.GONE
    }


    //初始化监听事件
    override fun initAction() {
        ivBack.setOnClickListener(this)
        tvCreate.setOnClickListener(this)
        tvStartDate.setOnClickListener(this)
        tvEndDate.setOnClickListener(this)
        switchAllday.setOnCheckedChangeListener(this)
        btnSave.setOnClickListener(this)
        btnShare.setOnClickListener(this)
    }
    //监听点击事件
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_back -> finish()
            R.id.tv_create ->{
                generateQR()
            }
            R.id.tvStartDate ->getDateTime(DATE_START)
            R.id.tvEndDate ->getDateTime(DATE_END)
            R.id.btnSave ->{
                calendarQRBitmap?.let { saveQR(it) }
            }
            R.id.btnShare ->{
                shareQR()
            }
        }
    }

    //监听全天选择按钮变化
    override fun onCheckedChanged(compoundButton: CompoundButton, isChecked: Boolean) {
        isAllDay = isChecked
        updateDateTime()
    }
    //初始化时间
    private fun initDateTime() {
        var start_date =
            getMonthText(calendarStart.get(Calendar.MONTH)) + " " + calendarStart.get(Calendar.DAY_OF_MONTH) + "  "
        var end_date =
            getMonthText(calendarEnd.get(Calendar.MONTH)) + " " + calendarEnd.get(Calendar.DAY_OF_MONTH) + "  "

        if (!isAllDay) {
            //如果不是全天模式要进行时分拼接
            val startMinute = calendarStart.get(Calendar.MINUTE)
            val startMinStr = startMinute.toString()
            val endMinute = calendarEnd.get(Calendar.MINUTE)
            val endMinStr = endMinute.toString()
            start_date += this@CalendarCreateActivity.calendarStart.get(Calendar.HOUR_OF_DAY).toString() + ":" + startMinStr
            end_date = calendarEnd.get(Calendar.HOUR_OF_DAY).toString() + ":" + endMinStr
        }
        //拼接初始时间
        tvStartDate.text = start_date
        tvEndDate.text = end_date
    }
    //获取当前日期时间
    private fun getDateTime(category: Int) {
        //1,时间选择器
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                if (category == DATE_START) {
                    calendarStart.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendarStart.set(Calendar.MINUTE, minute)
                } else {
                    calendarEnd.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendarEnd.set(Calendar.MINUTE, minute)
                }
                updateDateTime()
            },
            if (category == DATE_START) calendarStart.get(Calendar.HOUR_OF_DAY)
            else calendarEnd.get(Calendar.HOUR_OF_DAY),
            if (category == DATE_START) calendarStart.get(
                Calendar.MINUTE
            ) else calendarEnd.get(Calendar.MINUTE),
            true
        )
        //2，日期选择器
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                if (category == DATE_START) {
                    calendarStart.set(Calendar.YEAR, year)
                    calendarStart.set(Calendar.MONTH, month)
                    calendarStart.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                } else {
                    calendarEnd.set(Calendar.YEAR, year)
                    calendarEnd.set(Calendar.MONTH, month)
                    calendarEnd.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                if (!isAllDay) {
                    //如果不是全天模式，日期选择器调用时间选择器
                    timePickerDialog.show()
                }
                updateDateTime()
            },
            if (category == DATE_START) calendarStart.get(
                Calendar.YEAR
            ) else calendarEnd.get(Calendar.YEAR),
            if (category == DATE_START) calendarStart.get(
                Calendar.MONTH
            ) else calendarEnd.get(Calendar.MONTH),
            if (category == DATE_START) calendarStart.get(
                Calendar.DAY_OF_MONTH
            ) else calendarEnd.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    //更新日期时间
    @SuppressLint("DefaultLocale")
    private fun updateDateTime() {
        //1,获取时间月日
        var start_date =
            getMonthText(calendarStart.get(Calendar.MONTH)) + " " + calendarStart.get(Calendar.DAY_OF_MONTH) + "  "
        var end_date =
           getMonthText(calendarEnd.get(Calendar.MONTH)) + " " + calendarEnd.get(Calendar.DAY_OF_MONTH) + "  "

        if (!isAllDay) {
                //2,使用String.format自动补零(%02d表示两位数字不足补0)
                start_date += String.format("%02d:%02d",
                    calendarStart.get(Calendar.HOUR_OF_DAY),
                    calendarStart.get(Calendar.MINUTE))
                end_date += String.format("%02d:%02d",
                    calendarEnd.get(Calendar.HOUR_OF_DAY),
                    calendarEnd.get(Calendar.MINUTE))
        }

        if (calendarStart.get(Calendar.DAY_OF_MONTH) != calendarEnd.get(Calendar.DAY_OF_MONTH) ||
            calendarStart.get(Calendar.MONTH) != calendarEnd.get(Calendar.MONTH) ||
            calendarStart.get(Calendar.YEAR) != calendarEnd.get(Calendar.YEAR)
        ) {
            if (!isAllDay) {
                end_date = getMonthText(calendarEnd.get(Calendar.MONTH)) + " " +
                        calendarEnd.get(Calendar.DAY_OF_MONTH) + "  " + end_date
            }
        }

       //3,只要开始和结束年份不一致，或者不是今年，就显示年份
        val currentYear = calendarTemp.get(Calendar.YEAR)
        val startYear = calendarStart.get(Calendar.YEAR)
        val endYear = calendarEnd.get(Calendar.YEAR)
        if (startYear != currentYear || startYear != endYear) {
            start_date = "$startYear $start_date"
        }
        if (endYear != currentYear || startYear != endYear) {
            end_date = "$endYear $end_date"
        }

        tvStartDate.text = start_date
        tvEndDate.text = end_date
    }
    //创建二维码
    private fun generateQR() {
        calendarModel.title = etTitle.text.toString().trim()
        calendarModel.location = etLocation.text.toString().trim()
        calendarModel.description = etDescription.text.toString().trim()

        // 同步当前开始与结束时间 并赋值日历模型
        calendarModel.isAllDay = isAllDay
        calendarModel.startTime = calendarToLocalDateTime(calendarStart)
        calendarModel.endTime = calendarToLocalDateTime(calendarEnd)

        //检验数据是否有效
        if (!calendarModel.isValid()){
            if (calendarModel.title.isEmpty()){
                etTitle.error = getString(R.string.error_empty_title)
            }
            if (calendarModel.endTime.isBefore(calendarModel.startTime)){
                Toast.makeText(this,R.string.error_end_before_start, Toast.LENGTH_SHORT).show()
            }
            return
        }
        val qrContent = calendarModel.getQRContent()
        calendarQRBitmap = QRHelper.createQRBitmap(qrContent)
        if (calendarQRBitmap != null){
            ivQr.setImageBitmap(calendarQRBitmap)
            btnSave.visibility = View.VISIBLE
            btnShare.visibility = View.VISIBLE
            saveRecord()
            Toast.makeText(this,R.string.calendar_generated, Toast.LENGTH_SHORT).show()
            Toast.makeText(this,getString(R.string.toast_insert_calendar_record_success), Toast.LENGTH_SHORT).show()
        }else{
           //创建二维码失败提示
            Toast.makeText(this,R.string.error_generation_failed, Toast.LENGTH_SHORT).show()
        }
    }
    //保存创建二维码的历史记录
    private fun saveRecord() {
       val item = HistoryItem(
           title = calendarModel.title,
           content = calendarModel.getQRContent(),
           format = RecordType.CALENDAR,
           timestamp = System.currentTimeMillis()
           )
        dbManager.insert(HistoryDBManagerHelper.CREATE_TABLE_NAME,item)  //列表适配器自动刷新列表展示

    }

    //获取当前日历信息，用来
    private fun calendarToLocalDateTime(calendar: Calendar): LocalDateTime {
        return LocalDateTime.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            0
        )
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
        calendarQRBitmap?.let { bitmap ->
            PictureHelper.shareWith(this, bitmap)?.let { intent ->
                startActivity(Intent.createChooser(intent, getString(R.string.share)))
            } ?: Toast.makeText(this, getString(R.string.error_save_unknown), Toast.LENGTH_SHORT).show()
        }
    }

    //获取月的输出文本对象
    fun getMonthText(month: Int) =
        "Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec".split("_".toRegex()).toTypedArray()[month]

}
