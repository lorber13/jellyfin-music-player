package io.github.lorber13.jellyfinmusic.feature.login

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * The local network permission. From Android 17 (API 37), connections to addresses on the
 * user's network, like a home server at 192.168.1.10, are blocked until the user allows it.
 * On older versions the permission doesn't exist and access is always allowed.
 */
@Stable
internal class LocalNetworkAccess(
    private val context: Context,
    private val activity: Activity?,
    private val hasAskedState: MutableState<Boolean>,
    private val launcher: ActivityResultLauncher<String>
) {
    var isGranted by mutableStateOf(checkGranted())
        private set

    private var hasAsked by hasAskedState

    /** True the first time the user connects without the permission: the app asks once, up front. */
    val shouldAskBeforeConnecting: Boolean
        get() = !isGranted && !hasAsked

    fun ask() {
        hasAsked = true
        launcher.launch(PERMISSION)
    }

    /**
     * Asks again after the user said no. Once Android stops showing the dialog (after the
     * second "Don't allow"), the permission can only be changed in the app's system settings.
     */
    fun askAgainOrOpenSettings() {
        val systemStillShowsDialog = activity?.shouldShowRequestPermissionRationale(PERMISSION) ?: false
        if (hasAsked && !systemStillShowsDialog) {
            openAppSettings()
        } else {
            ask()
        }
    }

    /** Reads the permission again, e.g. after the user comes back from the system settings. */
    fun refresh() {
        isGranted = checkGranted()
    }

    private fun checkGranted(): Boolean =
        !isPermissionRequired() || context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        context.startActivity(intent)
    }

    private companion object {
        // Only asked for or checked on API 37+: below it, isGranted is always true, so neither
        // the first-connect request nor the hint button that asks again is ever reached.
        @SuppressLint("InlinedApi")
        const val PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CINNAMON_BUN)
        fun isPermissionRequired(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
    }
}

/** @param onAnswered called after the user answers the permission dialog, whatever the answer. */
@Composable
internal fun rememberLocalNetworkAccess(onAnswered: () -> Unit): LocalNetworkAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val hasAskedState = rememberSaveable { mutableStateOf(false) }
    val currentOnAnswered by rememberUpdatedState(onAnswered)
    // The permission dialog pauses the activity, so the resume effect below reads the new answer.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        currentOnAnswered()
    }
    val access = remember(context, activity, launcher) {
        LocalNetworkAccess(context, activity, hasAskedState, launcher)
    }
    LifecycleResumeEffect(access) {
        access.refresh()
        onPauseOrDispose { }
    }
    return access
}
