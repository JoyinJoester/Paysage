package joyin.takgi.paysage.esim

internal class EsimAidAttemptPlan(
    val aids: List<ByteArray>,
    private val shouldOpenMore: (openedAids: List<ByteArray>, nextAid: ByteArray) -> Boolean
) {
    fun shouldAttempt(openedAids: List<ByteArray>, nextAid: ByteArray): Boolean =
        shouldOpenMore(openedAids, nextAid)

    companion object {
        fun default(baseAids: List<ByteArray>): EsimAidAttemptPlan =
            EsimAidAttemptPlan(
                aids = baseAids.distinctBy { EsimApdu.aidHex(it) },
                shouldOpenMore = { openedAids, _ -> openedAids.isEmpty() }
            )

        fun estk(baseAids: List<ByteArray>): EsimAidAttemptPlan =
            EsimAidAttemptPlan(
                aids = EsimApdu.withEstkPreferredAids(baseAids),
                shouldOpenMore = { openedAids, nextAid ->
                    !(openedAids.isNotEmpty() && nextAid.contentEquals(EsimApdu.ISD_R_AID))
                }
            )
    }
}
