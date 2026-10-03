package com.gitls.uploader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest

object ApkInstaller {

    enum class Sig { MATCH, MISMATCH, NOT_INSTALLED, UNKNOWN }

    class Inspection(
        val pkg: String,
        val newName: String,
        val newCode: Long,
        val oldName: String?,
        val oldCode: Long?,
        val sig: Sig,
    ) {
        val isDowngrade: Boolean get() = oldCode != null && newCode < oldCode
        /** True bila Android seharusnya menerima APK ini sebagai update (atau pemasangan baru). */
        val canInstallInPlace: Boolean get() = sig != Sig.MISMATCH && !isDowngrade
    }

    private fun flags(): Int =
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else legacyFlags()

    @Suppress("DEPRECATION")
    private fun legacyFlags(): Int = PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun legacySignatures(info: PackageInfo): Array<Signature>? = info.signatures

    private fun signers(info: PackageInfo): Set<String> {
        val sigs: Array<Signature>? = if (Build.VERSION.SDK_INT >= 28) {
            val si = info.signingInfo
            when {
                si == null -> null
                si.hasMultipleSigners() -> si.apkContentsSigners
                else -> si.signingCertificateHistory
            }
        } else {
            legacySignatures(info)
        }
        return sigs.orEmpty().map { s ->
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    /** Membaca paket di dalam APK dan membandingkannya dengan versi yang sedang terpasang. */
    fun inspect(context: Context, apk: File): Inspection? {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, flags()) ?: return null
        val pkg = archive.packageName ?: return null
        val installed = try {
            pm.getPackageInfo(pkg, flags())
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val sig = when {
            installed == null -> Sig.NOT_INSTALLED
            else -> {
                val a = signers(archive)
                val b = signers(installed)
                when {
                    a.isEmpty() || b.isEmpty() -> Sig.UNKNOWN
                    a.intersect(b).isNotEmpty() -> Sig.MATCH
                    else -> Sig.MISMATCH
                }
            }
        }
        return Inspection(
            pkg = pkg,
            newName = archive.versionName ?: "?",
            newCode = PackageInfoCompat.getLongVersionCode(archive),
            oldName = installed?.versionName,
            oldCode = installed?.let { PackageInfoCompat.getLongVersionCode(it) },
            sig = sig,
        )
    }

    fun needsUnknownSourcesPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 26) {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            )
        }
    }

    /** Membuka pemasang sistem. Bila paket sama & tanda tangan sama, Android menampilkan "Update". */
    fun install(activity: Activity, apk: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }
}
