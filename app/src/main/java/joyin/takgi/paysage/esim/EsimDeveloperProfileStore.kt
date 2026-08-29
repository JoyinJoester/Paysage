package joyin.takgi.paysage.esim

import android.content.Context
import joyin.takgi.paysage.R

class EsimDeveloperProfileStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasProfiles(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun writeSampleProfiles() {
        preferences.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_ACTIVE_SUBSCRIPTION_ID, SAMPLE_PROFILE_IDS.first())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }

    fun activeSubscriptionId(): Int? =
        preferences.getInt(KEY_ACTIVE_SUBSCRIPTION_ID, SAMPLE_PROFILE_IDS.first())
            .takeIf { hasProfiles() }

    fun profiles(): List<EsimSubscriptionSummary> {
        if (!hasProfiles()) return emptyList()
        val activeId = activeSubscriptionId()
        return sampleProfiles().map { profile ->
            if (profile.subscriptionId == activeId) {
                profile.copy(
                    displayName = appContext.getString(
                        R.string.format_developer_esim_profile_current,
                        profile.displayName
                    )
                )
            } else {
                profile
            }
        }
    }

    fun switchTo(subscriptionId: Int): EsimDownloadStartResult? {
        if (!hasProfiles()) return null
        val target = sampleProfiles().firstOrNull { it.subscriptionId == subscriptionId }
            ?: return EsimDownloadStartResult(
                started = false,
                requestId = "",
                message = appContext.getString(R.string.message_developer_esim_fake_switch_not_found)
            )
        preferences.edit()
            .putInt(KEY_ACTIVE_SUBSCRIPTION_ID, target.subscriptionId)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        return EsimDownloadStartResult(
            started = true,
            requestId = "developer-fake-switch-${System.currentTimeMillis()}",
            message = appContext.getString(
                R.string.message_developer_esim_fake_switch_success,
                target.displayName,
                target.subscriptionId
            )
        )
    }

    private fun sampleProfiles(): List<EsimSubscriptionSummary> =
        listOf(
            sampleProfile(
                subscriptionId = SAMPLE_PROFILE_IDS[0],
                displayName = appContext.getString(R.string.developer_esim_profile_primary),
                carrierName = appContext.getString(R.string.developer_esim_carrier_lab),
                simSlotIndex = 0,
                portIndex = 0,
                countryIso = "CN"
            ),
            sampleProfile(
                subscriptionId = SAMPLE_PROFILE_IDS[1],
                displayName = appContext.getString(R.string.developer_esim_profile_travel),
                carrierName = appContext.getString(R.string.developer_esim_carrier_roaming),
                simSlotIndex = 0,
                portIndex = 1,
                countryIso = "JP"
            ),
            sampleProfile(
                subscriptionId = SAMPLE_PROFILE_IDS[2],
                displayName = appContext.getString(R.string.developer_esim_profile_backup),
                carrierName = appContext.getString(R.string.developer_esim_carrier_backup),
                simSlotIndex = 1,
                portIndex = 0,
                countryIso = "US"
            )
        )

    private fun sampleProfile(
        subscriptionId: Int,
        displayName: String,
        carrierName: String,
        simSlotIndex: Int,
        portIndex: Int,
        countryIso: String
    ): EsimSubscriptionSummary =
        EsimSubscriptionSummary(
            subscriptionId = subscriptionId,
            displayName = displayName,
            carrierName = carrierName,
            simSlotIndex = simSlotIndex,
            cardId = 9001,
            portIndex = portIndex,
            canManage = true,
            isEmbedded = true,
            countryIso = countryIso
        )

    companion object {
        private const val PREFS_NAME = "paysage_developer_esim_profiles"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ACTIVE_SUBSCRIPTION_ID = "active_subscription_id"
        private const val KEY_UPDATED_AT = "updated_at"
        private val SAMPLE_PROFILE_IDS = listOf(900101, 900102, 900103)
    }
}
