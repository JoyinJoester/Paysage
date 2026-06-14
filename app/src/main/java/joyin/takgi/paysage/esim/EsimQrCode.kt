package joyin.takgi.paysage.esim

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

class EsimQrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            EsimQrCodeDecoder.decode(image)?.let(onQrCodeDetected)
        } finally {
            image.close()
        }
    }
}

object EsimQrCodeDecoder {
    fun decode(context: Context, uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return null
        if (bytes.isEmpty()) return null

        for (sampleSize in sampleSizes(bytes)) {
            val bitmap = decodeBitmap(bytes, sampleSize) ?: continue
            try {
                val decoded = decode(bitmap)
                    ?: decodeRotated(bitmap, 90f)
                    ?: decodeRotated(bitmap, 180f)
                    ?: decodeRotated(bitmap, 270f)
                if (decoded != null) return decoded
            } finally {
                bitmap.recycle()
            }
        }
        return null
    }

    fun decode(bitmap: Bitmap): String? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return decodeSource(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))
    }

    fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        val data = ByteArray(width * height)
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var outputOffset = 0

        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (column in 0 until width) {
                data[outputOffset++] = buffer.get(rowStart + column * pixelStride)
            }
        }

        return decodeSource(
            PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
        )
    }

    private fun decodeSource(source: LuminanceSource): String? =
        decodeOnce(source, hybrid = true)
            ?: decodeOnce(source, hybrid = false)
            ?: decodeOnce(source.invert(), hybrid = true)
            ?: decodeOnce(source.invert(), hybrid = false)

    private fun decodeOnce(source: LuminanceSource, hybrid: Boolean): String? {
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.CHARACTER_SET to "UTF-8"
                )
            )
        }
        return try {
            val binarizer = if (hybrid) HybridBinarizer(source) else GlobalHistogramBinarizer(source)
            reader.decodeWithState(BinaryBitmap(binarizer)).text?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun sampleSizes(bytes: ByteArray, maxEdge: Int = 3072): List<Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return listOf(1)

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxEdge || bounds.outHeight / sampleSize > maxEdge) {
            sampleSize *= 2
        }

        val sizes = linkedSetOf(sampleSize)
        while (sampleSize > 1) {
            sampleSize /= 2
            sizes += sampleSize
        }
        sizes += 1
        return sizes.toList()
    }

    private fun decodeBitmap(bytes: ByteArray, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun decodeRotated(bitmap: Bitmap, angle: Float): String? {
        val matrix = Matrix().apply { postRotate(angle) }
        val rotated = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrNull() ?: return null
        return try {
            decode(rotated)
        } finally {
            rotated.recycle()
        }
    }
}
