package io.github.lorber13.jellyfinmusic

import android.app.PendingIntent
import android.content.ComponentName
import android.os.Build
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.lorber13.jellyfinmusic.core.playback.PlaybackService
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// In the app's tests, not the playback module's, because only the app has a launcher activity.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PlaybackSessionActivityTest {
    // The playback service gets its dependencies from Hilt, which needs a test component.
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun tappingTheMediaNotification_opensTheApp() {
        val sessionActivity = sessionActivityOfPlaybackService()

        assertNotNull("the playback session has no activity to open", sessionActivity)
        assertEquals(context.packageName, sessionActivity!!.creatorPackage)
        // Earlier versions can't tell what kind of intent a PendingIntent holds.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue("it should open an activity", sessionActivity.isActivity)
            assertTrue("other apps must not be able to change it", sessionActivity.isImmutable)
        }
    }

    /** Connects to the playback service like the notification does and reads its session activity. */
    private fun sessionActivityOfPlaybackService(): PendingIntent? {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        lateinit var controllerFuture: ListenableFuture<MediaController>
        // A controller is used on the thread it is built on, here the main thread.
        instrumentation.runOnMainSync {
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        }
        val controller = controllerFuture.get(10, TimeUnit.SECONDS)
        var sessionActivity: PendingIntent? = null
        instrumentation.runOnMainSync {
            sessionActivity = controller.sessionActivity
            controller.release()
        }
        return sessionActivity
    }
}
