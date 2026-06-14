package joyin.takgi.paysage.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun format(timestamp: Long): String = formatter.format(Date(timestamp))
}
