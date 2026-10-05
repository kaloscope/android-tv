package org.kaloscope.tv.feature.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import org.kaloscope.tv.BuildConfig
import org.kaloscope.tv.core.common.UpdateFailure

class AppUpdateFileProvider : FileProvider()

internal object AppUpdateInstaller {
    fun hasInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    @RequiresApi(Build.VERSION_CODES.O)
    fun openPermissionSettings(packageName: String, launch: (String, String?) -> Boolean): Boolean {
        // Some TV settings apps only expose a sources list or general security settings.
        val destinations = listOf(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to "package:$packageName",
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES to null,
            Settings.ACTION_SECURITY_SETTINGS to null,
            Settings.ACTION_SETTINGS to null,
        )
        return destinations.any { (action, data) -> launch(action, data) }
    }

    @Suppress("DEPRECATION")
    fun validate(context: Context, apk: File, version: String): UpdateFailure? {
        if (!apk.isFile || apk.length() == 0L) return UpdateFailure.FileMissing
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val manager = context.packageManager
        val archive = manager.getPackageArchiveInfo(apk.path, flags)
            ?: return UpdateFailure.IncompatiblePackage
        if (archive.packageName != context.packageName || archive.versionName != version ||
            PackageInfoCompat.getLongVersionCode(archive) <= BuildConfig.VERSION_CODE ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                (archive.applicationInfo?.minSdkVersion ?: 0) > Build.VERSION.SDK_INT)
        ) {
            return UpdateFailure.IncompatiblePackage
        }
        val installed = manager.getPackageInfo(context.packageName, flags)
        val currentSigners = signers(installed, history = false)
        val acceptedSigners = signers(archive, history = true)
        if (currentSigners.isEmpty() || !acceptedSigners.containsAll(currentSigners) ||
            (currentSigners.size > 1 && currentSigners != acceptedSigners)
        ) {
            return UpdateFailure.SignatureMismatch
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo, history: Boolean): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { signing ->
                if (history && !signing.hasMultipleSigners()) signing.signingCertificateHistory
                else signing.apkContentsSigners
            }
        } else {
            info.signatures
        }
        return signatures.orEmpty().map { it.toCharsString() }.toSet()
    }

    @Suppress("DEPRECATION")
    fun intent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .apply { clipData = ClipData.newRawUri("APK", uri) }
    }
}
