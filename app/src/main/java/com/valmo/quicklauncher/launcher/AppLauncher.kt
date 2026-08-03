package com.valmo.quicklauncher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.valmo.quicklauncher.model.AppShortcut

sealed interface LaunchResult {
    data object Success : LaunchResult

    data class Unavailable(val label: String) : LaunchResult

    data class Failed(val label: String) : LaunchResult
}

class AppLauncher(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun isAvailable(shortcut: AppShortcut): Boolean =
        runCatching {
            createIntent(shortcut)?.resolveActivity(packageManager) != null
        }.onFailure { error ->
            Log.w(TAG, "Could not resolve ${shortcut.packageName}", error)
        }.getOrDefault(false)

    fun loadIcon(shortcut: AppShortcut): Drawable? =
        try {
            packageManager.getApplicationIcon(shortcut.packageName)
        } catch (error: PackageManager.NameNotFoundException) {
            Log.i(TAG, "No icon found for ${shortcut.packageName}")
            null
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not load icon for ${shortcut.packageName}", error)
            null
        }

    fun launch(shortcut: AppShortcut): LaunchResult {
        val intent = try {
            createIntent(shortcut)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Could not create intent for ${shortcut.packageName}", error)
            return LaunchResult.Failed(shortcut.label)
        }

        if (intent == null || intent.resolveActivity(packageManager) == null) {
            Log.w(TAG, "No launchable activity for ${shortcut.packageName}")
            return LaunchResult.Unavailable(shortcut.label)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return try {
            appContext.startActivity(intent)
            LaunchResult.Success
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "Activity disappeared for ${shortcut.packageName}", error)
            LaunchResult.Unavailable(shortcut.label)
        } catch (error: SecurityException) {
            Log.e(TAG, "Launch blocked for ${shortcut.packageName}", error)
            LaunchResult.Failed(shortcut.label)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unexpected launch failure for ${shortcut.packageName}", error)
            LaunchResult.Failed(shortcut.label)
        }
    }

    private fun createIntent(shortcut: AppShortcut): Intent? =
        shortcut.launchAction
            ?.let(::Intent)
            ?: packageManager.getLaunchIntentForPackage(shortcut.packageName)

    private companion object {
        const val TAG = "QuickLauncher"
    }
}

