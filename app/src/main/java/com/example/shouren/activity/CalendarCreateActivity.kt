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
import com.example.shouren.functions.createFunction.CalendarModel
import com.example.shouren.utils.PictureHelper
import com.example.shouren.utils.QRHelper
import java.time.LocalDateTime

class CalendarCreateActivity: BaseActivity(), View.OnClickListener,  CompoundButton.OnCheckedChangeListener {
    //1,延迟初始化控件
    private lateinit var iv_back: ImageView
    private lateinit var tv_create: TextView
    private lateinit var et_title: EditText
    private lateinit var et_location: EditText
    private lateinit var et_description: EditText
    private lateinit var tv_start_date: TextView
    private lateinit var tv_end_date: TextView


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchAllday: Switch
    private  var isAllDay = false
    private lateinit var calendar_start: Calendar
    private lateinit var calendar_end: Calendar
    private lateinit var calendar_temp: Calendar
    private lateinit var iv_qr: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    //引入日历模型
    private val calendarModel = CalendarModel()
    //引入二维码位图
    private var calendarQRBitmap: Bitmap ?= null

    companion object {
        private const val DATE_START = 1
        private const val DATE_END = 2
    }
    //初始化布局
    override fun getLayout(): Int = R.layout.activity_calendar_create
    //初始化数据逻辑
    @SuppressLint("CutPasteId")
    override fun initData() {
        iv_back = findViewById(R.id.iv_back)
        tv_create = findViewById(R.id.tv_create)
        et_title = findViewById(R.id.etTitle)
        et_location = findViewById(R.id.etLocation)
        et_description = findViewById(R.id.etDescription)
        tv_start_date = findViewById(R.id.tvStartDate)
        tv_end_date = findViewById(R.id.tvEndDate)
        switchAllday = findViewById(R.id.switchAllDay) // Added missing initialization
        iv_qr = findViewById(R.id.iv_qr)
        btnSave = findViewById(R.id.btnSave)
        btnShare =findViewById(R.id.btnShare)

        calendar_start = Calendar.getInstance();
        calendar_end = Calendar.getInstance()
        calendar_end.add(Calendar.HOUR_OF_DAY,1)
        calendar_temp = Calendar.getInstance()
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
        iv_back.setOnClickListener(this)
        tv_create.setOnClickListener(this)
        tv_start_date.setOnClickListener(this)
        tv_end_date.setOnClickListener(this)
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
            getMonthText(calendar_start.get(Calendar.MONTH)) + " " + calendar_start.get(Calendar.DAY_OF_MONTH) + "  "
        var end_date =
            getMonthText(calendar_end.get(Calendar.MONTH)) + " " + calendar_end.get(Calendar.DAY_OF_MONTH) + "  "

        if (!isAllDay) {
            //如果不是全天模式要进行时分拼接
            val startMinute = calendar_start.get(Calendar.MINUTE)
            val startMinStr = startMinute.toString()
            val endMinute = calendar_end.get(Calendar.MINUTE)
            val endMinStr = endMinute.toString()
            start_date += calendar_start.get(Calendar.HOUR_OF_DAY).toString() + ":" + startMinStr
            end_date = calendar_end.get(Calendar.HOUR_OF_DAY).toString() + ":" + endMinStr
        }
        //拼接初始时间
        tv_start_date.text = start_date
        tv_end_date.text = end_date
    }
    //获取当前日期时间
    private fun getDateTime(category: Int) {
        //1,时间选择器
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                if (category == DATE_START) {
                    calendar_start.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar_start.set(Calendar.MINUTE, minute)
                } else {
                    calendar_end.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar_end.set(Calendar.MINUTE, minute)
                }
                updateDateTime()
            },
            if (category == DATE_START) calendar_start.get(Calendar.HOUR_OF_DAY)
            else calendar_end.get(Calendar.HOUR_OF_DAY),
            if (category == DATE_START) calendar_start.get(
                Calendar.MINUTE
            ) else calendar_end.get(Calendar.MINUTE),
            true
        )
        //2，日期选择器
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                if (category == DATE_START) {
                    calendar_start.set(Calendar.YEAR, year)
                    calendar_start.set(Calendar.MONTH, month)
                    calendar_start.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                } else {
                    calendar_end.set(Calendar.YEAR, year)
                    calendar_end.set(Calendar.MONTH, month)
                    calendar_end.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                if (!isAllDay) {
                    //如果不是全天模式，日期选择器调用时间选择器
                    timePickerDialog.show()
                }
                updateDateTime()
            },
            if (category == DATE_START) calendar_start.get(
                Calendar.YEAR
            ) else calendar_end.get(Calendar.YEAR),
            if (category == DATE_START) calendar_start.get(
                Calendar.MONTH
            ) else calendar_end.get(Calendar.MONTH),
            if (category == DATE_START) calendar_start.get(
                Calendar.DAY_OF_MONTH
            ) else calendar_end.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    //更新日期时间
    @SuppressLint("DefaultLocale")
    private fun updateDateTime() {
        //1,获取时间月日
        var start_date =
            getMonthText(calendar_start.get(Calendar.MONTH)) + " " + calendar_start.get(Calendar.DAY_OF_MONTH) + "  "
        var end_date =
           getMonthText(calendar_end.get(Calendar.MONTH)) + " " + calendar_end.get(Calendar.DAY_OF_MONTH) + "  "

        if (!isAllDay) {
                //2,使用String.format自动补零(%02d表示两位数字不足补0)
                start_date += String.format("%02d:%02d",
                    calendar_start.get(Calendar.HOUR_OF_DAY),
                    calendar_start.get(Calendar.MINUTE))
                end_date += String.format("%02d:%02d",
                    calendar_end.get(Calendar.HOUR_OF_DAY),
                    calendar_end.get(Calendar.MINUTE))
        }

        if (calendar_start.get(Calendar.DAY_OF_MONTH) != calendar_end.get(Calendar.DAY_OF_MONTH) ||
            calendar_start.get(Calendar.MONTH) != calendar_end.get(Calendar.MONTH) ||
            calendar_start.get(Calendar.YEAR) != calendar_end.get(Calendar.YEAR)
        ) {
            if (!isAllDay) {
                end_date = getMonthText(calendar_end.get(Calendar.MONTH)) + " " +
                        calendar_end.get(Calendar.DAY_OF_MONTH) + "  " + end_date
            }
        }

       //3,只要开始和结束年份不一致，或者不是今年，就显示年份
        val currentYear = calendar_temp.get(Calendar.YEAR)
        val startYear = calendar_start.get(Calendar.YEAR)
        val endYear = calendar_end.get(Calendar.YEAR)
        if (startYear != currentYear || startYear != endYear) {
            start_date = "$startYear $start_date"
        }
        if (endYear != currentYear || startYear != endYear) {
            end_date = "$endYear $end_date"
        }

        tv_start_date.text = start_date
        tv_end_date.text = end_date
    }
    //创建二维码
    private fun generateQR() {
        calendarModel.title = et_title.text.toString().trim()
        calendarModel.location = et_location.text.toString().trim()
        calendarModel.description = et_description.text.toString().trim()

        // 同步当前开始与结束时间 并赋值日历模型
        calendarModel.isAllDay = isAllDay
        calendarModel.startTime = calendarToLocalDateTime(calendar_start)
        calendarModel.endTime = calendarToLocalDateTime(calendar_end)

        //检验数据是否有效
        if (!calendarModel.isValid()){
            if (calendarModel.title.isEmpty()){
                et_title.error = getString(R.string.error_empty_title)
            }
            if (calendarModel.endTime.isBefore(calendarModel.startTime)){
                Toast.makeText(this,R.string.error_end_before_start, Toast.LENGTH_SHORT).show()
            }
            return
        }
        val qrContent = calendarModel.getQRContent()
        calendarQRBitmap = QRHelper.createQRBitmap(qrContent)
        if (calendarQRBitmap != null){
            iv_qr.setImageBitmap(calendarQRBitmap)
            btnSave.visibility = View.VISIBLE
            btnShare.visibility = View.VISIBLE
            Toast.makeText(this,R.string.calendar_generated, Toast.LENGTH_SHORT).show()
        }else{
           //创建二维码失败提示
            Toast.makeText(this,R.string.error_generation_failed, Toast.LENGTH_SHORT).show()
        }
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
