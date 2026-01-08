package pl.test.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.Px
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import androidx.core.graphics.scale

/*
    magazyn zdjec plikow
    zdjecia zapisywane jako jpg w filesdir aplikacji
    w bazie room trzymana tylko sciezka
*/

object ImageStore {
    private const val DIR_NAME = "results_images"

    //zapisuje bitmape jako JPG
    fun saveJpeg(context: Context, bitmap: Bitmap, maxSidePx: Int = 1280, quality: Int = 92): String {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }

        //nazwa pliku zapisywana z timestamp
        val timeStamp = System.currentTimeMillis()
        val file = File(dir, "img_${timeStamp}.jpg")

        //ograniczenie pamieci
        val scaled = downscaleIfNeeded(bitmap, maxSidePx)

        FileOutputStream(file).use{out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return file.absolutePath
    }

    //usuwa jedno zdjecie
    fun delete(path: String?){
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    //usuwa wszystkie zapisane zdjecia
    fun deleteAll(context: Context){
        val dir = File(context.filesDir, DIR_NAME)
        if (dir.exists()){
            runCatching { dir.deleteRecursively() }
        }
    }

    private fun downscaleIfNeeded(src: Bitmap, maxSide: Int): Bitmap{
        val width = src.width
        val height = src.height
        val maxNow = maxOf(width, height)

        if (maxNow <= maxSide) return src

        val scale = maxSide.toFloat()/maxNow.toFloat()
        val newWidth = (width*scale).roundToInt().coerceAtLeast(1)
        val newHeight = (height*scale).roundToInt().coerceAtLeast(1)

        return src.scale(newWidth, newHeight)
    }
}