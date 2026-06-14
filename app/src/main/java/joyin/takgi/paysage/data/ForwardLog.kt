package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forward_logs")
data class ForwardLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val emailSuccess: Boolean,
    val telegramSuccess: Boolean,
    val filtered: Boolean = false
)
