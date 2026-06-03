package com.example.shouren.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PictureHelper {
    private const val FILE_FOLDER = "SHOUREN_QR"
    private const val PREFIX = "QR_"
    
      //保存二维码图片到相册
    fun savePicture(context: Context, bitmap: Bitmap): SaveInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAfterQ(context, bitmap)
        } else {
            saveBeforeQ(context, bitmap)
        }
    }

    //Android 10 以后保存方式  ->使用MediaStore
    private fun saveAfterQ(context: Context, bitmap: Bitmap): SaveInfo {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, generateFileName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/$FILE_FOLDER")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(it, contentValues, null, null)
                SaveInfo.Success(it.toString())
            } catch (e: Exception) {
                SaveInfo.Error(e.message ?: "Unknown error")
            }
        } ?: SaveInfo.Error("Failed to create media entry")
    }

    //Android 10之前方式保存图片 ->mediaStore
    private fun saveBeforeQ(context: Context, bitmap: Bitmap): SaveInfo {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PermissionChecker.PERMISSION_GRANTED) {
            return SaveInfo.PermissionRequired
        }

        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString() + "/$FILE_FOLDER"

        val folder = File(imagesDir)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, generateFileName())
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // 通知媒体扫描器
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = android.net.Uri.fromFile(file)
            }
            context.sendBroadcast(mediaScanIntent)
            SaveInfo.Success(file.absolutePath)
        } catch (e: Exception) {
            SaveInfo.Error(e.message ?: "Unknown error")
        }
    }

    //生成带时间戳的文件名
    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "$PREFIX$timestamp.png"
    }

    //创建分享二维码的Intent
    fun shareWith(context: Context, bitmap: Bitmap): Intent? {
        return try {
            // 保存图片到缓存目录
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(shareDir, "qr_share_${System.currentTimeMillis()}.png")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // 使用 FileProvider 获取 URI
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            // 创建分享 Intent
            return Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    sealed class SaveInfo {
        data class Success(val path: String) : SaveInfo()
        data class Error(val message: String) : SaveInfo()
        object PermissionRequired : SaveInfo()
    }
}
