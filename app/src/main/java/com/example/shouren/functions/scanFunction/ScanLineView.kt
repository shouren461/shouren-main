package com.example.shouren.functions.scanFunction

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

//扫码激光动画，定义一个扫描的激光视图，模拟上下扫动的激光效果
class ScanLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet ?= null,
    defStyleAttr: Int = 0
): View(context,attrs,defStyleAttr){
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG) //画笔开启抗锯齿没有扫描线更丝滑
    //激光线动画状态变量
    private var lasery = 0f  //激光线纵坐标高度
    private val laserSpeed = 6f  //激光线速度
    private val laserHeight = 280f  //渐变色块总高度
    private val laserLineHeight = 80f  //扫描线高度
    //预先定义激光扫描矩阵 和渐变效果
    private val laserMatrix = Matrix()
    private val laserShade : Shader by lazy {
        LinearGradient(
        0f,0f,      //激光线起始位置 (0,0)
        0f,laserHeight, //激光线结束位置，默认高度
        intArrayOf(     //颜色渐变由“透明”变为"绿色"再变为"透明"
            Color.TRANSPARENT,
            "#10b981".toColorInt(),
            Color.TRANSPARENT),
        floatArrayOf(0f,0.5f,1f),  //定义颜色出现的位置两边暗中间亮
        Shader.TileMode.CLAMP   //若绘制区域超过渐变范围，默认使用透明色填充
    ) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //1,循环扫描->激光线到底反弹到扫描框上部
        if (lasery > height){
            lasery = 0f
        }
        //2,不断更新laserShader位置矩阵Matrix，应用渐变效果，无需频繁地创建laserShade对象
        laserMatrix.setTranslate(0f,lasery)
        laserShade.setLocalMatrix(laserMatrix)
        paint.shader = laserShade
        //3,绘制矩阵激光线矩形视图,其高度就是我们的激光线高度
        canvas.drawRect(0f,lasery,width.toFloat(),lasery+laserLineHeight,paint)
        //4,自我驱动动画->更新下一帧的纵坐标并请求下一帧动画时自动重绘
        lasery += laserSpeed
        postInvalidateOnAnimation()
    }
}