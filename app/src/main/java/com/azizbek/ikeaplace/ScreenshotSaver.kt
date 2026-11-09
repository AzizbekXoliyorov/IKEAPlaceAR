package com.azizbek.ikeaplace

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object ScreenshotSaver {
    private const val FOLDER = "IKEAPlaceAR"

    /** Copies the real window pixels (camera + 3D content) with PixelCopy. */
    suspend fun capture(activity: Activity): Bitmap? = suspendCancellableCoroutine { cont ->
        val decor = activity.window.decorView
        if (decor.width == 0 || decor.height == 0) { cont.resume(null); return@suspendCancellableCoroutine }
        val bmp = try { Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888) }
        catch (e: OutOfMemoryError) { cont.resume(null); return@suspendCancellableCoroutine }
        try {
            PixelCopy.request(activity.window, bmp, { result ->
                if (cont.isActive) cont.resume(if (result == PixelCopy.SUCCESS) bmp else null)
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) { if (cont.isActive) cont.resume(null) }
    }

    /** Saves a bitmap to Pictures/IKEAPlaceAR via MediaStore. Returns the Uri or null on failure. */
    fun save(context: Context, bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val name = "IKEAPlaceAR_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
                dir.mkdirs()
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, File(dir, name).absolutePath)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val compressed = resolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            } ?: error("no output stream")
            check(compressed) { "bitmap compression failed" }
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null); null
        }
    }

    /** Lists images saved by this app, newest first. */
    fun list(context: Context): List<Uri> {
        val out = mutableListOf<Uri>()
        val q29 = Build.VERSION.SDK_INT >= 29
        @Suppress("DEPRECATION")
        val column = if (q29) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        val arg = if (q29) "Pictures/$FOLDER%" else "%/Pictures/$FOLDER/%"
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID),
                "$column LIKE ?", arrayOf(arg), "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                while (c.moveToNext()) out += Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0).toString())
            }
        } catch (_: Exception) { }
        return out
    }

    fun decode(context: Context, uri: Uri, sample: Int = 4): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    } catch (_: Exception) { null }
}
