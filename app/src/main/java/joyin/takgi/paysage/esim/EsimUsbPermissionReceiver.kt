package joyin.takgi.paysage.esim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

class EsimUsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_USB_PERMISSION) return

        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_DEVICE_NAME, deviceName)
            .putBoolean(KEY_LAST_GRANTED, granted)
            .putLong(KEY_LAST_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val ACTION_USB_PERMISSION = "joyin.takgi.paysage.esim.action.USB_PERMISSION"
        const val EXTRA_DEVICE_NAME = "joyin.takgi.paysage.esim.extra.USB_DEVICE_NAME"
        private const val PREFS_NAME = "paysage_esim_usb_permission"
        private const val KEY_LAST_DEVICE_NAME = "last_device_name"
        private const val KEY_LAST_GRANTED = "last_granted"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
    }
}
