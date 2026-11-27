package io.github.lorber13.jellyfinmusic.core.designsystem.format

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented because android.icu only exists on a device.
@RunWith(AndroidJUnit4::class)
class TrackDurationFormatTest {
    @Test
    fun trackDuration_isAClockReadingWithoutALeadingZero() {
        assertEquals("3:45", formatTrackDuration(3.minutes + 45.seconds, Locale.US))
        assertEquals("0:07", formatTrackDuration(7.seconds, Locale.US))
        assertEquals("1:02:03", formatTrackDuration(62.minutes + 3.seconds, Locale.US))
    }
}
