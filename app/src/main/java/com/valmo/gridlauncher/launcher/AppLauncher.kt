package com.valmo.gridlauncher.launcher

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.valmo.gridlauncher.model.AppShortcut

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Unavailable(val label: String) : LaunchResult
    data class Failed(val label: String) : LaunchResult
}

class AppLauncher(application: Application) {
    private val appContext = application.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val packageManager = appContext.packageManager
    private val userHandle = Process.myUserHandle()

    fun availableApps(): List<AppShortcut> =
        runCatching {
            launcherApps.getActivityList(null, userHandle)
                .asSequence()
                .filter { it.componentName.packageName != appContext.packageName }
                .distinctBy { it.componentName.packageName }
                .map { info ->
                    AppShortcut(
                        packageName = info.componentName.packageName,
                        label = info.label.toString(),
                    )
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }.onFailure { error ->
            Log.e(TAG, "Could not enumerate launchable apps", error)
        }.getOrDefault(emptyList())

    fun resolveShortcut(
        packageName: String,
        fallbackLabel: String,
        launchAction: String? = null,
    ): AppShortcut {
        val label = runCatching {
            launcherApps.getActivityList(packageName, userHandle)
                .firstOrNull()
                ?.label
                ?.toString()
        }.getOrNull()
            ?: runCatching {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            }.getOrNull()
            ?: fallbackLabel

        return AppShortcut(
            packageName = packageName,
            label = label,
            launchAction = launchAction,
        )
    }

    fun isAvailable(shortcut: AppShortcut): Boolean =
        if (shortcut.launchAction != null) {
            runCatching {
                Intent(shortcut.launchAction).resolveActivity(packageManager) != null
            }.getOrDefault(false)
        } else {
            runCatching {
                launcherApps.getActivityList(shortcut.packageName, userHandle).isNotEmpty()
            }.getOrDefault(false)
        }

    fun launch(shortcut: AppShortcut): LaunchResult {
        return try {
            if (shortcut.launchAction != null) {
                val intent = Intent(shortcut.launchAction)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(packageManager) == null) {
                    return LaunchResult.Unavailable(shortcut.label)
                }
                appContext.startActivity(intent)
            } else {
                val activity = launcherApps.getActivityList(shortcut.packageName, userHandle)
                    .firstOrNull()
                    ?: return LaunchResult.Unavailable(shortcut.label)
                launcherApps.startMainActivity(activity.componentName, userHandle, null, null)
            }
            LaunchResult.Success
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "No launchable activity for ${shortcut.packageName}", error)
            LaunchResult.Unavailable(shortcut.label)
        } catch (error: SecurityException) {
            Log.e(TAG, "Launch blocked for ${shortcut.packageName}", error)
            LaunchResult.Failed(shortcut.label)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unexpected launch failure for ${shortcut.packageName}", error)
            LaunchResult.Failed(shortcut.label)
        }
    }

    fun openAppInfo(packageName: String): Boolean =
        startSystemActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ),
            packageName,
            "app info",
        )

    fun requestUninstall(packageName: String): Boolean =
        startSystemActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.parse("package:$packageName"),
            ),
            packageName,
            "uninstall",
        )

    private fun startSystemActivity(
        intent: Intent,
        packageName: String,
        actionName: String,
    ): Boolean =
        try {
            appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "No activity available for $actionName: $packageName", error)
            false
        } catch (error: SecurityException) {
            Log.e(TAG, "$actionName blocked for $packageName", error)
            false
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unexpected $actionName failure for $packageName", error)
            false
        }

    private companion object {
        const val TAG = "GridLauncher"
    }
}
