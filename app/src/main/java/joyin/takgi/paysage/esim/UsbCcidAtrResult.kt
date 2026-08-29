package joyin.takgi.paysage.esim

data class UsbCcidAtrResult(
    val success: Boolean,
    val message: String,
    val atrHex: String?
)
