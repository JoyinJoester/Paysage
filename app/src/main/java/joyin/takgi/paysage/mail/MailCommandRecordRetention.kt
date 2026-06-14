package joyin.takgi.paysage.mail

object MailCommandRecordRetention {
    const val RETENTION_DAYS: Long = 30
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    const val RETENTION_MS: Long = RETENTION_DAYS * DAY_MS

    fun cutoffMillis(nowMillis: Long): Long =
        (nowMillis - RETENTION_MS).coerceAtLeast(0L)
}

