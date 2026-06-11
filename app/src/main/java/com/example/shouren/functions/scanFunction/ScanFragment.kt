package com.example.shouren.functions.scanFunction

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.shouren.R
import com.example.shouren.activity.ScanItemDetailActivity
import com.example.shouren.database.HistoryDBManagerHelper
import com.example.shouren.database.HistoryItem
import com.example.shouren.database.RecordType
import com.example.shouren.functions.createFunction.CalendarModel
import com.example.shouren.functions.createFunction.YoutubeModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/*
关于扫描操作 ->
1,调用CameraX库 cameraX库用于 ->启动相机 ,preview ->用于视图预览,imageAnalysis ->用于抓取画面数据
2,调用Ml kit Barcode Scanning库执行解析操作
3，扫码成功后执行异步出库history.db 并且跳转到结果页ScanItemDetail
* */
class ScanFragment : Fragment(){
    private lateinit var preview: PreviewView   //相机预览UI控件
    //private lateinit var scanLine: View         //简单的扫描动画 ->已使用ScanLineView渐变扫描线代替
    private lateinit var ivCapture: ImageView
    private lateinit var ivFlash: ImageView
    private lateinit var scaleGestureDetector: ScaleGestureDetector  //手势缩放检测器
    private lateinit var clickGestureDetector: GestureDetector       //普通点击事件检测器
    private var isScanning = false                //标记扫描状态，为true标志相机是否正在扫描上一个码,避免连续跳转
    private val scanner: BarcodeScanner by lazy { BarcodeScanning.getClient() }  //获取Ml Kit 核心扫描客户端
    private val cameraExecutor = Executors.newSingleThreadExecutor()        //线程池:专门用来imageAnalysis来使用，避免阻塞主线程(UI线程)
    private val dbManager by lazy { HistoryDBManagerHelper(requireContext(),1) } //懒加载数据库管理器，添加扫描历史记录
    private var imageCapture: ImageCapture ?= null   //拍照用例
    private var cameraControl: CameraControl ?= null  //硬件控制(打开闪光灯)
    private var cameraInfo: CameraInfo ?= null
    private var isTorchOn = false     //标志闪光灯的状态
    //定义相册选择启动器  ->用户选择一个图片返回后，会触发这个回调
    private  var pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri->
        uri?.let{processImageFromUri(it)}
    }
    //定义权限请求启动器  ->规定发送请求权限后如何处理后续操作
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        isGrant ->
        if (isGrant){
            startCamera()
        }else{
            Toast.makeText(requireContext(),getString(R.string.toast_require_camera_permission), Toast.LENGTH_SHORT).show()
        }
    }

    //完成Fragment初始化操作
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //绑定view视图
        val view = inflater.inflate(R.layout.fragment_scan_camerax, container, false)
        //1,绑定UI控件
        preview =view.findViewById(R.id.previewView)
        //scanLine = view.findViewById(R.id.scanLine)
        ivFlash = view.findViewById(R.id.ivFlash)
        ivCapture = view.findViewById(R.id.ivCapture)

        //2,绑定相册按钮点击事件
        view.findViewById<View>(R.id.btnToAlbum).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        //3,给拍照按钮设置点击事件
        ivCapture.setOnClickListener {
            takePhoto()
        }
        //4,给闪光灯按钮设置点击事件
        ivFlash.setOnClickListener {
            toggleFlash()
        }
        return view
    }

    //完成视图创建后行为处理
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //3,检查相机启动权限然后决定是否扫描
        checkAndStartCamera()
        //开启相机成功后显示扫描线动画效果  ,已经被ScanLineView渐变扫描线动画效果代替
        //startAnimation()
        //4,开启缩放手势检测器初始化
        initZoomGestureDetector()
        //6,开启普通点击事件检测器初始化
        initClickGesture()
        //5,为preview视图设置触摸监听
        setupTouchListener()
    }
    //检查权限然后决定是否扫描
    private fun checkAndStartCamera() {
        val cameraPermission = Manifest.permission.CAMERA
        //1,情况1:用户有相机权限,直接开启相机扫描与二维码解析
        when{
            ContextCompat.checkSelfPermission(requireContext(),cameraPermission) == PackageManager.PERMISSION_GRANTED ->{
                startCamera()
            }
            else ->{//2,情况2:用户没有权限，向系统请求相机权限
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }

    //初始化CameraX摄像头 ->我们CameraX时用例驱动的，需要定义显示用例和隐式用例
    private fun startCamera() {
        //1,获取相机提供者的异步实例
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            //2,拿到正真的相机管理对象cameraProvider ->用于将生命周期绑定到生命周期所有者
            val cameraProvider = cameraProviderFuture.get()
               //2.1 创建预览用例(previewer) ->用于将相机获取的每一帧图像刷到preview视图上
            val preview =  Preview.Builder().build().also {
                it.setSurfaceProvider(preview.surfaceProvider)
            }
               //2.2 创建分析用例 ->(imageAnalysis) ->将获取的每一帧图像处理解析
            val imageAnalysis = ImageAnalysis.Builder().
            setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)//设置图片处理策略默认是处理最后一帧，避免重复跳转
                .build().also {
                it.setAnalyzer(cameraExecutor){imageProxy ->   //设置分析器 ->告诉cameraX在线程池(cameraExecutor)中处理我们的imageProxy帧画面
                    processImageProxy(imageProxy)
                }
            }
            //3,创建相机用例
             imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()  //注重摄像模式为最低延迟

            //4,默认选择后置摄像头
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            //5,先解绑所有用例  然后重新绑定预览(previewer)和分析用例到相机的生命周期上
            try {
                cameraProvider.unbindAll()
                val camera =  cameraProvider.bindToLifecycle(this,cameraSelector,preview,imageAnalysis,imageCapture)
                cameraControl = camera.cameraControl  //拿到相机控制对象
                cameraInfo = camera.cameraInfo   //获取到相机参数信息
            }catch (e: Exception){//打印相机启动失败信息
                Toast.makeText(requireContext(),getString(R.string.toast_camera_boot_failed), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    //处理动态相机抛出的每一帧图像 ->imageProxy代表原始数据包裹
    @SuppressLint("UnsafeOptInUsageError")  //imageProxy.image方法尚处于实验性阶段 ->忽略可能存在的错误异常
    private fun processImageProxy(imageProxy: ImageProxy) {
        //状态检查 ->如果为false说明已经被扫描且跳转，丢弃后续所有帧
        if (isScanning){
            imageProxy.close()
            return
        }
        //1,从imageProxy包裹中取出须需要的的mediaImage图片资源
       val mediaImage =  imageProxy.image ?: run{
            imageProxy.close()
            return
        }
        //2,将获取的imageProxy包装成Ml Kit能识别的InputImage格式
        val inputImage =
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        //3,扫描引擎解析图片
        scanner.process(inputImage).addOnSuccessListener {barcodes ->
            if (barcodes.isNotEmpty()){ //如果barcodes不为空，说明imageProxy中已经存在一个或多个二维码
                isScanning = true
                val result = barcodes[0].rawValue ?:""
                processScanResult(result)  //处理二维码原始字符串
            }
        }.addOnCompleteListener {
            //4,扫描结束后，要释放imageProxy帧的资源，释放内存
            imageProxy.close()
        }
    }

    //处理从相册获取的静态二维码图片
    private fun processImageFromUri(uri: Uri) {
        //inputImage支持直接从本地文件路径构造
        val image = InputImage.fromFilePath(requireContext(), uri)
        scanner.process(image).addOnSuccessListener {barcodes ->
            if (barcodes.isNotEmpty()){
                val result = barcodes[0].rawValue ?:""
                processScanResult(result)  //相册扫码成功，走同样的业务逻辑
            }else{
                //如果解析失败，弹出提示文本
                Toast.makeText(requireContext(),getString(R.string.toast_not_detected_QRCode),
                    Toast.LENGTH_SHORT).show()
            }
        }

    }

    //处理从相机或者相册扫码之后的核心数据处理逻辑
    private fun processScanResult(initResult: String) {
        //1,识别传入的二维码类型(Youtube，日历或者普通文本)
           //1.1 根据传入的文本来确定type类型
        val type = when{
            YoutubeModel.isYoutubeLink(initResult) -> RecordType.YOUTUBE
            initResult.contains("BEGIN:VCALENDAR") -> RecordType.CALENDAR
            else -> RecordType.TEXT
        }
           //1.2根据类型type来确定标题
       var title = when(type){
           RecordType.YOUTUBE  ->{
               val youtubeType = YoutubeModel.identifyYoutubeType(initResult)
               YoutubeModel(youtubeType,initResult).getId()
           }
           RecordType.CALENDAR->
                   CalendarModel.fromString(initResult) ?.title  ?: getString(R.string.calendar_events)
           else -> {
               if (initResult.length >20) initResult.take(20 ) + "..." else initResult
           }
       }
        //1.3 设置兜底方案，对于未识别到表题的文本设置默认标题
        if (title.isBlank()){
            title = when(type){
                RecordType.YOUTUBE ->"Youtube视频"
                RecordType.CALENDAR ->"日历事件"
                else -> "扫描文本"
            }
        }
        //2,数据库操作:开启协程将扫描结果异步保到数据库中
        lifecycleScope.launch(Dispatchers.IO) {
            dbManager.insert(HistoryDBManagerHelper.SCAN_TABLE_NAME,
                HistoryItem(title= title, content = initResult, format = type)
            )
            //3,结果反馈:返回主线程并跳转到结果页
            withContext(Dispatchers.Main){
                ScanItemDetailActivity.startActivity(requireContext(),initResult,type)
            }
        }
    }

    //开启扫描动画效果 ->扫描线位移简单动画，模拟扫描线上下扫描的效果,现在已经被scanLineView渐变扫描线动画效果代替
   /* private fun startAnimation() {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT,0f,
            Animation.RELATIVE_TO_PARENT,0f,
            Animation.RELATIVE_TO_PARENT,0f,
            Animation.RELATIVE_TO_PARENT,0.95f,  //向下移动95%的高度
        ).apply { //设置扫描动画效果 ->周期2000ms,无限循环，到头重来
            duration = 2000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.RESTART
        }
        scanLine.startAnimation(animation)
    }*/

    //处理闪光灯开关操作
    private fun toggleFlash(){
        isTorchOn = !isTorchOn
        cameraControl?.enableTorch(isTorchOn)  //调动enableTorch()API实现闪光灯操作
        //切换图标状态
        ivFlash.setImageResource(if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
    }

    //实现相机拍照操作
    private fun takePhoto(){
        //1，定义照片的名字和存储位置
        val name = "Scan_Capture_${System.currentTimeMillis()}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,name)
            put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P){ //Android 10+以上指定存储到“图片/Camera”目录下
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/SHOUREN_QR")
            }
        }

        //2,创建输出选项，指定系统的mediaStore
        val outputOptions = ImageCapture.OutputFileOptions.Builder(requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)
            .build()
        //3,执行拍照逻辑
        imageCapture?.takePicture(outputOptions,cameraExecutor,object :ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputfileResults: ImageCapture.OutputFileResults) {
                lifecycleScope.launch(Dispatchers.Main) {
                    //拍照和保存图片属于异步操作，isAdded检查当前的ScanFragment是否还在Activity上，防止回调时调用requireContext()或getString()找不到Context导致的类型转换异常
                    if (isAdded){
                        //打印拍照成功提示和刷新相册
                        Toast.makeText(requireContext(),getString(R.string.toast_photo_token_successfully),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onError(exception: ImageCaptureException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (isAdded){
                        //打印拍照失败提示
                        val errorMsg = "拍照失败:${exception.message}(错误码:${exception.imageCaptureError})"
                        Toast.makeText(requireContext(),errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    //初始化缩放手势检测器
    private fun initZoomGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener(){  //初始化手势检测器
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    //1,检查缩放状态
                    val zoomState = cameraInfo?.zoomState?.value ?: return false
                    //2,获取当前缩放比例信息
                    val currentZoomState = zoomState.zoomRatio
                    //3,根据当前缩放信息来设置相机缩放状态(目标缩放状态 = 现在的缩放比例 * 检测器监听到的手势变化因子
                    //detector.scaleFactor>1:放大画面 <1:缩小画面)
                    val targetZoomState = currentZoomState * detector.scaleFactor
                    //4,设置相机到标准的缩放比例
                    cameraControl?.setZoomRatio(targetZoomState)
                    return true
                }
            }
        )
    }
    //初始化普通点击事件检测器
    private fun initClickGesture() {
        clickGestureDetector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {//设置双击恢复原比例操作
                    cameraControl?.setZoomRatio(1.0f)
                    return true
                }
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {  //设置单击设置聚焦功能
                    //1,获取点击位置的工厂类
                    val factory = preview.meteringPointFactory
                    //2,将点击位置的(x,y)转换为相机对焦的坐标
                    val point = factory.createPoint(e.x, e.y)
                    //3,设置对焦动作，包含对焦(FLAG_AF)和自动曝光(FLAG_AE)
                    val action = FocusMeteringAction.Builder(point,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    //4,下发对焦指令
                    cameraControl?.startFocusAndMetering(action)
                    return true
                }
            })
    }
    //为preview视图设置触摸监听
    @SuppressLint("ClickableViewAccessibility") //请忽略关于"无障碍辅助功能的警告"
    private fun setupTouchListener() {
        preview.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event) //1,将监听的手势动作转发给缩放手势检测器
            clickGestureDetector.onTouchEvent(event) //2,将监听的普通点击事件转发给普通点击检测器
            true     //该触摸事件已经被消费
        }
    }
    override fun onResume() {
        super.onResume()
        isScanning = false //每次返回扫描页重置相机扫描效果
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()  //销毁页面时，关闭线程池和扫码器,防止内存泄漏
        scanner.close()
    }

}