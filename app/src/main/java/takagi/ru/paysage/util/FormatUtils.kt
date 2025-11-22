package takagi.ru.paysage.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import takagi.ru.paysage.R
import kotlin.math.pow

/**
 * 格式化工具函数
 */
object FormatUtils {
    
    /**
     * 格式化文件大小
     * 将字节转换为人类可读的格式（B, KB, MB, GB）
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        val size = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format("%.1f %s", size, units[digitGroups])
    }
    
    /**
     * 格式化相对时间
     * 将时间戳转换为相对时间描述（如"2小时前"）
     */
    fun formatRelativeTime(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365
        
        return when {
            seconds < 60 -> context.getString(R.string.book_detail_just_now)
            minutes < 60 -> context.getString(R.string.book_detail_minutes_ago, minutes.toInt())
            hours < 24 -> context.getString(R.string.book_detail_hours_ago, hours.toInt())
            days < 7 -> context.getString(R.string.book_detail_days_ago, days.toInt())
            weeks < 4 -> context.getString(R.string.book_detail_weeks_ago, weeks.toInt())
            months < 12 -> context.getString(R.string.book_detail_months_ago, months.toInt())
            else -> context.getString(R.string.book_detail_years_ago, years.toInt())
        }
    }
    
    /**
     * 复制文本到剪贴板
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
