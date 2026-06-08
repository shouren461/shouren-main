package com.example.shouren.functions.scanFunction

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.AudioSpec
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.shouren.R
import com.example.shouren.activity.BaseFragment
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType
import com.example.shouren.functions.createFunction.CalendarModel
import com.example.shouren.functions.createFunction.YoutubeModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//扫描界面 ->使用Zxing Android Embedded 第三方库集成，库自动处理相机相机流和扫描UI动画
class ScanFragment: Fragment() {
    //Zxing库提供的复合控件(包含预览，扫描框，激光线)
    private lateinit var barCodeView: DecoratedBarcodeView
    private lateinit var btnAlbum: FloatingActionButton
    //懒加载数据管理器
    private val dbManager by lazy { HistoryDBManagerHelper(requireContext(),1) }
    //定义权限请求启动器 -> 规定发起请求权限后如何处理结果
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){isGranted ->
        if (isGranted){  //用户在系统弹窗中点击了允许,执行扫码操作
            startScanning()
        }else{            //用户在系统弹窗中点击了拒绝，弹出无相机权限提醒
            Toast.makeText(requireContext(),getString(R.string.toast_require_camera_permission), Toast.LENGTH_SHORT).show()
        }
    }
    //定义相册选择启动器 ->使用ActivityResultContracts.getContent()方法来调起系统文件选择器
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri ->
        uri?.let { decodeFromUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //创建视图view
        val view = inflater.inflate(R.layout.fragment_scan, container, false)
        //1,绑定布局中的自定义控件
        barCodeView = view.findViewById(R.id.view_scan)
        //2,绑定相册按钮
        btnAlbum = view.findViewById(R.id.btnAlbum)
        btnAlbum.setOnClickListener { pickImageLauncher.launch("image/*") }  //发起选择图片的请求
        return  view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndStartScanning()   //检查权限逻辑
    }

    //检查当前是否拥有相机权限
    private fun checkAndStartScanning() {
        val cameraPermission = Manifest.permission.CAMERA
        //使用ContextCompat 检查当前的授权状态
        when{
            ContextCompat.checkSelfPermission(requireContext(),cameraPermission) ==
                    PackageManager.PERMISSION_GRANTED ->{
                        //情况1:如果已经授权，直接开启相机扫描和预览
                        startScanning()
            }
            else ->{
                //情况2：未授权，发起相机权限请求
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
    //调用 ZXing 库的方法进行实时扫描
    private fun startScanning() {
        //decodeContinus是连续扫描模式 ->每识别到一个二维码到会触发自动回调
        barCodeView.decodeContinuous(object :BarcodeCallback{
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    barCodeView.pause()   //每识别到一个二维码后要暂停扫描，否则回弹出多个结果页
                    processScanResult(it.text)
                }
            }
        })
    }

    //处理扫码成功之后的数据逻辑
    private fun processScanResult(initResult: String) {
        //1,识别类型:根据内容特征来判断是视频还是日历,还是文本
        val type = when {
            YoutubeModel.isYoutubeLink(initResult) -> RecordType.YOUTUBE
            initResult.contains("BEGIN:VCALENDAR") -> RecordType.CALENDAR
            else -> RecordType.TEXT
        }
        //加载标题
        var title = when (type) {
            RecordType.YOUTUBE -> {
                val detectedType = YoutubeModel.detectType(initResult)
                YoutubeModel(type = detectedType, input = initResult).getId()
            }
            RecordType.CALENDAR -> CalendarModel.fromString(initResult)?.title
                ?: getString(R.string.calendar_events)

            else -> if (initResult.length > 20) initResult.take(20) + "..." else initResult
        }

        // 兜底方案：如果解析出来的标题为空（例如非标准链接），强制赋予一个默认标题
        if (title.isBlank()) {
            title = when(type) {
                RecordType.YOUTUBE -> "YouTube 视频"
                RecordType.CALENDAR -> "日历事件"
                else -> "扫描文本"
            }
        }

        //2,数据库操作:开启协程，在IO线程异步保存
        lifecycleScope.launch(Dispatchers.IO) {
            dbManager.insert(
                HistoryDBManagerHelper.SCAN_TABLE_NAME,
                HistoryItem(title = title, content = initResult, format = type)
            )
            //3,结果反馈，回到主线程跳转到结果页
            withContext(Dispatchers.Main) {
                ScanItemDetail.startActivity(requireContext(), initResult, type)
            }
        }
    }
    // 手动解析相册图片中二维码 ->虽然库自动处理了相机流，但解析静态图片仍然需要Zxing的原始Api
    private fun decodeFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO){
            try {
                //1,通过ContentResolver 将URL转换为位图bitmap
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                //2,提取像素数据到整数数组
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                //3,构造Zxing识别所需的特定对象
                val source = RGBLuminanceSource(bitmap.width,bitmap.height,pixels) //RGBLuminanceSource:将颜色图转换为亮度图(识别二维码只需要黑白信息)
                val binaryizer = HybridBinarizer(source) //HybridBinarizer:混合二值化器，进一步处理光影，提供识别率
                val binaryBitmap = BinaryBitmap(binaryizer) //BinaryBitmap:最终交给解析器处理的黑白二值图
                //4,调用MutiFormat执行解析
                val result = MultiFormatReader().decode(binaryBitmap)
                //5,成功拿到文本，走统一处理流程
                withContext(Dispatchers.Main){
                    processScanResult(result.text)
                }
            }catch (e: Exception){
                //如果图片中没码或者解析报错，弹出提示
                withContext(Dispatchers.Main){
                    Toast.makeText(requireContext(),getString(R.string.toast_detected_QRCode),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //生命周期管理 ->同步库的生命周期，否则相机回一直占用
    override fun onResume() {
        super.onResume()
        //只有在已经授权的情况下才能恢复预览
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            barCodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        //页面不可见时暂停相机，节省资源
        barCodeView.pause()
    }

}