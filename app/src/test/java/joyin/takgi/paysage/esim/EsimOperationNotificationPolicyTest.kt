package joyin.takgi.paysage.esim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsimOperationNotificationPolicyTest {
    @Test
    fun operationNotificationSettingsAreAppliedByRequestPrefix() {
        val settings = EsimUserSettings(
            notifyDownloadOperations = false,
            notifySwitchOperations = false,
            notifyDeleteOperations = true,
            notifyRenameOperations = false
        )

        assertFalse(EsimOperationNotificationPolicy.shouldNotify(settings, "download-1"))
        assertFalse(EsimOperationNotificationPolicy.shouldNotify(settings, "switch-1"))
        assertFalse(EsimOperationNotificationPolicy.shouldNotify(settings, "switch-port-0-1"))
        assertTrue(EsimOperationNotificationPolicy.shouldNotify(settings, "delete-1"))
        assertFalse(EsimOperationNotificationPolicy.shouldNotify(settings, "rename-1"))
    }

    @Test
    fun unknownOperationsStillNotifyByDefault() {
        val settings = EsimUserSettings(
            notifyDownloadOperations = false,
            notifySwitchOperations = false,
            notifyDeleteOperations = false,
            notifyRenameOperations = false
        )

        assertTrue(EsimOperationNotificationPolicy.shouldNotify(settings, "future-operation-1"))
    }
}
