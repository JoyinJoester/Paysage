package joyin.takgi.paysage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_forward_messages")
data class PendingForwardMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sender: String,
    val content: String,
    val smsTimestamp: Long,
    val source: String,
    val attempts: Int = 0,
    val lastError: String = "",
    val nextAttemptAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
