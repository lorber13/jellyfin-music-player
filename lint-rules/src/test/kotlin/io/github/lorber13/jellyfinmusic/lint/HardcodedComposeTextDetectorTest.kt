package io.github.lorber13.jellyfinmusic.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class HardcodedComposeTextDetectorTest {
    @Test
    fun literalPassedToComposable_isReported() {
        lintScreen(
            """
            @Composable
            fun Screen() {
                Text("Play")
                Icon(contentDescription = "Pause")
            }
            """
        ).expect(
            """
            src/test/pkg/Screen.kt:14: Warning: Hardcoded text can't be translated: move it to strings.xml and use stringResource [HardcodedComposeText]
                Text("Play")
                     ~~~~~~
            src/test/pkg/Screen.kt:15: Warning: Hardcoded text can't be translated: move it to strings.xml and use stringResource [HardcodedComposeText]
                Icon(contentDescription = "Pause")
                                          ~~~~~~~
            0 errors, 2 warnings
            """
        )
    }

    @Test
    fun templateAndConcatenation_areReported() {
        lintScreen(
            """
            @Composable
            fun Screen(albumNumber: Int) {
                Text("Album ${'$'}albumNumber")
                Text("Track " + albumNumber)
            }
            """
        ).expect(
            """
            src/test/pkg/Screen.kt:14: Warning: Hardcoded text can't be translated: move it to strings.xml and use stringResource [HardcodedComposeText]
                Text("Album ${'$'}albumNumber")
                     ~~~~~~~~~~~~~~~~~~~~
            src/test/pkg/Screen.kt:15: Warning: Hardcoded text can't be translated: move it to strings.xml and use stringResource [HardcodedComposeText]
                Text("Track " + albumNumber)
                     ~~~~~~~~~~~~~~~~~~~~~~
            0 errors, 2 warnings
            """
        )
    }

    @Test
    fun resourcesVariablesAndTextWithoutLetters_areClean() {
        lintScreen(
            """
            @Composable
            fun Screen(albumTitle: String, trackCount: Int) {
                Text(stringResource(1))
                Text(albumTitle)
                Text("${'$'}trackCount")
                Text("·")
                Text("")
                Icon(contentDescription = null)
            }
            """
        ).expectClean()
    }

    @Test
    fun previews_areClean() {
        lintScreen(
            """
            @Preview
            @Composable
            fun ScreenPreview() {
                Text("Play")
            }

            @Preview
            annotation class ThemePreviews

            @ThemePreviews
            @Composable
            fun ThemedScreenPreview() {
                Text("Play")
            }
            """
        ).expectClean()
    }

    @Test
    fun nonComposableFunction_isClean() {
        lintScreen(
            """
            fun log(message: String) {}

            fun record() {
                log("Playback started")
            }
            """
        ).expectClean()
    }

    @Test
    fun semanticsContentDescription_isReported_butTestTagIsNot() {
        lintScreen(
            """
            fun describe() {
                semantics {
                    contentDescription = "Album cover"
                    testTag = "albumCover"
                }
            }
            """
        ).expect(
            """
            src/test/pkg/Screen.kt:14: Warning: Hardcoded text can't be translated: move it to strings.xml and use stringResource [HardcodedComposeText]
                    contentDescription = "Album cover"
                                         ~~~~~~~~~~~~~
            0 errors, 1 warnings
            """
        )
    }

    /** Runs the check on [code], placed in a file that already imports the Compose stubs. */
    private fun lintScreen(code: String) = lint()
        .files(
            *composeStubs,
            kotlin(
                "src/test/pkg/Screen.kt",
                """
                package test.pkg

                import androidx.compose.material3.Icon
                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.res.stringResource
                import androidx.compose.ui.semantics.contentDescription
                import androidx.compose.ui.semantics.semantics
                import androidx.compose.ui.semantics.testTag
                import androidx.compose.ui.tooling.preview.Preview

                """.trimIndent() + "\n" + code.trimIndent()
            )
        )
        .issues(HardcodedComposeTextDetector.ISSUE)
        .allowMissingSdk()
        .run()

    // Signature-only copies of the Compose APIs the check looks at.
    private val composeStubs: Array<TestFile> = arrayOf(
        kotlin(
            """
            package androidx.compose.runtime

            annotation class Composable
            """
        ).indented(),
        kotlin(
            """
            package androidx.compose.ui.tooling.preview

            annotation class Preview
            """
        ).indented(),
        kotlin(
            """
            package androidx.compose.material3

            import androidx.compose.runtime.Composable

            @Composable
            fun Text(text: String) {}

            @Composable
            fun Icon(contentDescription: String?) {}
            """
        ).indented(),
        kotlin(
            """
            package androidx.compose.ui.res

            import androidx.compose.runtime.Composable

            @Composable
            fun stringResource(id: Int): String = ""
            """
        ).indented(),
        kotlin(
            """
            package androidx.compose.ui.semantics

            class SemanticsPropertyReceiver

            var SemanticsPropertyReceiver.contentDescription: String
                get() = ""
                set(value) {}

            var SemanticsPropertyReceiver.testTag: String
                get() = ""
                set(value) {}

            fun semantics(properties: SemanticsPropertyReceiver.() -> Unit) {}
            """
        ).indented()
    )
}
