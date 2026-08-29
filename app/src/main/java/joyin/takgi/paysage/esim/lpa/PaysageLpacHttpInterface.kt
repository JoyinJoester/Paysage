package joyin.takgi.paysage.esim.lpa

import kotlinx.coroutines.flow.flowOf
import net.typeblog.lpac_jni.HttpInterface
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl

class PaysageLpacHttpInterface : HttpInterface {
    private val delegate = HttpInterfaceImpl(
        verboseLoggingFlow = flowOf(false),
        ignoreTLSCertificateFlow = flowOf(false)
    )

    override fun transmit(
        url: String,
        tx: ByteArray,
        headers: Array<String>
    ): HttpInterface.HttpResponse =
        delegate.transmit(url, tx, headers)

    override fun usePublicKeyIds(pkids: Array<String>) {
        delegate.usePublicKeyIds(pkids)
    }
}
