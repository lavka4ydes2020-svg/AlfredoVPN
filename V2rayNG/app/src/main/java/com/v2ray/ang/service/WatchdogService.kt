package com.v2ray.ang.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.enums.NotificationChannelType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watchdog service for Kill Switch.
 *
 * Monitors VPN connection health when Kill Switch is enabled.
 * If the VPN drops (core stops unexpectedly):
 * 1. Attempts to restart the VPN automatically
 * 2. Shows a notification about the protection status
 *
 * Runs as a foreground service to prevent the system from killing it.
 * Auto-stops when VPN is manually disconnected.
 *
 * Design notes:
 * - Uses manual CoroutineScope instead of LifecycleService to avoid
 *   requiring the lifecycle-service dependency.
 * - Uses START_STICKY so the system recreates the service if killed.
 * - The service checks kill switch state on each poll cycle, so toggling
 *   the pref off will cause a clean exit within POLL_INTERVAL_MS.
 */
class WatchdogService : Service() {

    companion object {
        private const val TAG = "WatchdogService"
        private const val POLL_INTERVAL_MS = 3000L
        private const val RESTART_DELAY_MS = 2000L
        private const val MAX_RESTART_ATTEMPTS = 5
    }

    private var scope: CoroutineScope? = null
    private var monitorJob: Job? = null
    private var restartAttempts = 0

    override fun onCreate() {
        super.onCreate()
        LogUtil.i(TAG, "Watchdog service created")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Start foreground notification
        NotificationHelper.startForeground(
            service = this,
            channelType = NotificationChannelType.WATCHDOG,
            title = getString(R.string.watchdog_notification_title),
            content = getString(R.string.watchdog_notification_active)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i(TAG, "Watchdog service start command received")

        // If kill switch is now disabled, stop self
        if (!MmkvManager.decodeKillSwitch()) {
            LogUtil.i(TAG, "Kill Switch is disabled, stopping watchdog")
            stopSelf()
            return START_NOT_STICKY
        }

        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        LogUtil.i(TAG, "Watchdog service destroyed")
        monitorJob?.cancel()
        monitorJob = null
        scope?.cancel()
        scope = null
        NotificationHelper.stopForeground(this)
        super.onDestroy()
    }

    /**
     * Starts the VPN monitoring loop.
     * Checks CoreServiceManager.isRunning() periodically.
     * If VPN drops, attempts to restart.
     */
    private fun startMonitoring() {
        monitorJob?.cancel()
        restartAttempts = 0

        val currentScope = scope ?: return
        monitorJob = currentScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)

                // If kill switch was disabled, stop
                if (!MmkvManager.decodeKillSwitch()) {
                    LogUtil.i(TAG, "Kill Switch disabled during monitoring, stopping")
                    stopSelf()
                    return@launch
                }

                val isRunning = CoreServiceManager.isRunning()

                if (!isRunning) {
                    LogUtil.w(TAG, "VPN is not running! Attempting restart...")
                    onVpnDisconnected()
                } else {
                    // Reset restart attempts on healthy state
                    if (restartAttempts > 0) {
                        restartAttempts = 0
                        NotificationHelper.updateNotification(
                            NotificationChannelType.WATCHDOG,
                            this@WatchdogService,
                            getString(R.string.watchdog_notification_active)
                        )
                    }
                }
            }
        }
    }

    /**
     * Called when VPN disconnection is detected.
     * Attempts to restart the VPN and updates notification.
     */
    private fun onVpnDisconnected() {
        restartAttempts++
        LogUtil.w(TAG, "VPN disconnected (attempt $restartAttempts/$MAX_RESTART_ATTEMPTS)")

        // Update notification to show alarm state
        val statusText = getString(
            R.string.watchdog_notification_restoring,
            restartAttempts,
            MAX_RESTART_ATTEMPTS
        )
        NotificationHelper.updateNotification(
            NotificationChannelType.WATCHDOG,
            this@WatchdogService,
            statusText
        )

        if (restartAttempts > MAX_RESTART_ATTEMPTS) {
            LogUtil.e(TAG, "Max restart attempts reached. Giving up.")
            NotificationHelper.updateNotification(
                NotificationChannelType.WATCHDOG,
                this@WatchdogService,
                getString(R.string.watchdog_notification_failed)
            )
            // Keep the foreground notification but stop trying
            // Blocking via setBlocking(true) in VpnService already blocks all traffic
            return
        }

        // Try to restart the VPN
        try {
            // Small delay to allow things to settle
            Thread.sleep(RESTART_DELAY_MS)
            CoreServiceManager.startVService(this)
            LogUtil.i(TAG, "VPN restart initiated")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Failed to restart VPN", e)
        }
    }
}
