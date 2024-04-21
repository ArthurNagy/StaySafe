package com.arthurnagy.staysafe.feature.shared

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.view.InflateException
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.arthurnagy.staysafe.R
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val M = "M"
private const val MONTH_FULL = "$M$M$M$M"
private const val DAY = "d"
private const val YEAR = "yyyy"

inline fun consume(block: () -> Unit) = true.also { block() }

inline fun doIfAboveVersion(version: Int, block: () -> Unit) {
    if (Build.VERSION.SDK_INT > version) {
        block()
    }
}

fun showSnackbar(view: View, anchorView: View? = null, message: Int, action: Int = 0, func: (() -> Unit)? = null, onDismissed: (() -> Unit)? = null) {
    try {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (action != 0 && func != null) {
            snackbar.setAction(action) {
                func()
            }.setActionTextColor(view.context.color(R.color.color_secondary))
        }
        if (anchorView != null) {
            snackbar.anchorView = anchorView
        }
        onDismissed?.let {
            snackbar.addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, @DismissEvent event: Int) {
                        onDismissed()
                    }
                }
            )
        }
        snackbar.show()
    } catch (exception: InflateException) {
        Timber.e(exception)
    }
}

fun formatToDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("$MONTH_FULL $DAY$DAY, $YEAR"))

fun formatToFormalDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("$DAY$DAY.$M$M.$YEAR"))

fun isToday(timestamp: Long): Boolean =
    Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.DAYS) == Instant.ofEpochMilli(MaterialDatePicker.todayInUtcMilliseconds())
        .truncatedTo(ChronoUnit.DAYS)

fun <T1, T2> mediatorLiveData(
    defaultValue: T1? = null,
    dependency: LiveData<T2>,
    onChanged: (currentValue: T1?, value: T2) -> T1?
): MutableLiveData<T1> =
    MediatorLiveData<T1>().also { mediatorLiveData ->
        defaultValue?.let { mediatorLiveData.value = it }
        mediatorLiveData.addSource(dependency) {
            onChanged(mediatorLiveData.value, it)?.let { newValue ->
                if (mediatorLiveData.value != newValue) {
                    mediatorLiveData.value = newValue
                }
            }
        }
    }

fun openUrl(context: Context, url: String) {
    if (getCustomTabsPackages(context).isNotEmpty()) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
                .setToolbarColor(context.color(R.color.color_primary))
                .build())
            .setShowTitle(true)
            .apply {
                context.drawable(R.drawable.ic_back_24dp)?.let {
                    setCloseButtonIcon(it.toBitmap())
                }
            }
            .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
            .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}

private fun getCustomTabsPackages(context: Context): List<ResolveInfo> {
    val pm = context.packageManager
    // Get default VIEW intent handler.
    val activityIntent = Intent(ACTION_VIEW, Uri.parse("https://www.example.com"))
    // Get all apps that can handle VIEW intents.
    val resolvedActivityList: List<ResolveInfo> = pm.queryIntentActivities(activityIntent, 0)
    val packagesSupportingCustomTabs = mutableListOf<ResolveInfo>()
    resolvedActivityList.forEach { info ->
        val serviceIntent = Intent().apply {
            action = ACTION_CUSTOM_TABS_CONNECTION
            setPackage(info.activityInfo.packageName)
        }
        // Check if this package also resolves the Custom Tabs service.
        if (pm.resolveService(serviceIntent, 0) != null) {
            packagesSupportingCustomTabs.add(info)
        }
    }
    return packagesSupportingCustomTabs
}