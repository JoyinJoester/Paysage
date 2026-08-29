package joyin.takgi.paysage.esim.lpa

import net.typeblog.lpac_jni.HttpInterface

class ReadOnlyLpacHttpInterface : HttpInterface {
    override fun transmit(
        url: String,
        tx: ByteArray,
        headers: Array<String>
    ): HttpInterface.HttpResponse {
        throw UnsupportedOperationException("HTTP is not available for read-only local profile operations.")
    }

    override fun usePublicKeyIds(pkids: Array<String>) = Unit
}
