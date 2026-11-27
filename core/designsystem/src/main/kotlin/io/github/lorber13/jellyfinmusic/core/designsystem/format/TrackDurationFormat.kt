package io.github.lorber13.jellyfinmusic.core.designsystem.format

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale
import kotlin.time.Duration

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

/**
 * A track's length as a clock reading in the user's language, such as "3:45" or "1:02:03".
 * Unlike `DateUtils.formatElapsedTime`, minutes have no leading zero, as in other music apps.
 */
fun formatTrackDuration(duration: Duration, locale: Locale): String {
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val measures = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        add(Measure(minutes, MeasureUnit.MINUTE))
        add(Measure(seconds, MeasureUnit.SECOND))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.NUMERIC).formatMeasures(*measures.toTypedArray())
}
