package com.valmo.gridlauncher.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.valmo.gridlauncher.launcher.AppLauncher
import com.valmo.gridlauncher.launcher.LaunchResult
import com.valmo.gridlauncher.model.AppShortcut
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.launcherDataStore by preferencesDataStore(name = "gridlauncher_preferences")

interface LauncherRepository {
    suspend fun configuredShortcuts(): List<AppShortcut>
    suspend fun availableApps(): List<AppShortcut>
    suspend fun saveConfiguredPackages(packageNames: List<String>)
    fun isAvailable(shortcut: AppShortcut): Boolean
    fun launch(shortcut: AppShortcut): LaunchResult
    fun openAppInfo(packageName: String): Boolean
    fun requestUninstall(packageName: String): Boolean
}

class AndroidLauncherRepository(
    application: Application,
) : LauncherRepository {
    private val appContext = application.applicationContext
    private val appLauncher = AppLauncher(application)

    override suspend fun configuredShortcuts(): List<AppShortcut> =
        readConfiguredPackages().map { packageName ->
            val default = DefaultShortcuts.configFor(packageName)
            appLauncher.resolveShortcut(
                packageName = packageName,
                fallbackLabel = default?.let { appContext.getString(it.fallbackLabelRes) }
                    ?: packageName.substringAfterLast('.'),
                launchAction = default?.launchAction,
            )
        }

    override suspend fun availableApps(): List<AppShortcut> = appLauncher.availableApps()

    override suspend fun saveConfiguredPackages(packageNames: List<String>) {
        appContext.launcherDataStore.edit { preferences ->
            preferences[SHORTCUT_PACKAGES] = packageNames
                .distinct()
                .joinToString(separator = "\n")
        }
    }

    override fun isAvailable(shortcut: AppShortcut): Boolean =
        appLauncher.isAvailable(shortcut)

    override fun launch(shortcut: AppShortcut): LaunchResult =
        appLauncher.launch(shortcut)

    override fun openAppInfo(packageName: String): Boolean =
        appLauncher.openAppInfo(packageName)

    override fun requestUninstall(packageName: String): Boolean =
        appLauncher.requestUninstall(packageName)

    private suspend fun readConfiguredPackages(): List<String> =
        try {
            val preferences = appContext.launcherDataStore.data.first()
            val parsed = preferences[SHORTCUT_PACKAGES]
                ?.lineSequence()
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.distinct()
                ?.toList()
                .orEmpty()

            parsed.ifEmpty { DefaultShortcuts.items.map(DefaultShortcut::packageName) }
        } catch (error: IOException) {
            Log.w(TAG, "Could not read launcher preferences", error)
            DefaultShortcuts.items.map(DefaultShortcut::packageName)
        }

    private companion object {
        val SHORTCUT_PACKAGES = stringPreferencesKey("shortcut_packages_v1")
        const val TAG = "GridLauncher"
    }
}
