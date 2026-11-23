package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lorber13.jellyfinmusic.core.data.library.Album
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSortField
import io.github.lorber13.jellyfinmusic.core.data.library.LibraryFailure
import io.github.lorber13.jellyfinmusic.core.data.library.SortDirection
import io.github.lorber13.jellyfinmusic.core.designsystem.theme.JellyfinMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumsContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val albums = listOf(
        Album(id = "1", name = "Morning Light", artistName = "Aurora Fields", coverImageUrl = null),
        Album(id = "2", name = "Steam & Rust", artistName = "The Copper Kettles", coverImageUrl = null)
    )

    @Test
    fun showsTheAlbumsWithTheirArtists() {
        showContent()

        composeRule.onNode(hasText("Morning Light")).assertExists()
        composeRule.onNode(hasText("Aurora Fields")).assertExists()
        composeRule.onNode(hasText("Steam & Rust")).assertExists()
        composeRule.onNode(hasText("The Copper Kettles")).assertExists()
    }

    @Test
    fun tappingAnAlbum_opensIt() {
        var openedAlbumId: String? = null
        showContent(onAlbumClick = { openedAlbumId = it })

        composeRule.onNode(hasText("Steam & Rust") and hasClickAction()).performClick()

        assertEquals("2", openedAlbumId)
    }

    @Test
    fun sortMenu_marksTheSelectedOptionAndItsDirection() {
        showContent(uiState = AlbumsUiState.Loaded(albums, AlbumSort(AlbumSortField.YEAR, SortDirection.DESCENDING)))

        composeRule.onNode(hasContentDescription(string(R.string.feature_albums_sort))).performClick()

        composeRule.onNode(menuItem(R.string.feature_albums_sort_name) and isSelected(false)).assertExists()
        composeRule.onNode(menuItem(R.string.feature_albums_sort_year) and isSelected(true)).assertExists()
        composeRule.onNode(menuItem(R.string.feature_albums_sort_date_added) and isSelected(false)).assertExists()
        composeRule.onNode(
            hasContentDescription(string(R.string.feature_albums_sort_newest_first)),
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun pickingASortOption_selectsItAndClosesTheMenu() {
        var selected: AlbumSortField? = null
        showContent(onSelectSort = { selected = it })

        composeRule.onNode(hasContentDescription(string(R.string.feature_albums_sort))).performClick()
        composeRule.onNode(menuItem(R.string.feature_albums_sort_date_added)).performClick()

        assertEquals(AlbumSortField.DATE_ADDED, selected)
        composeRule.onNode(menuItem(R.string.feature_albums_sort_date_added)).assertDoesNotExist()
    }

    @Test
    fun emptyLibrary_saysSo() {
        showContent(uiState = AlbumsUiState.Loaded(emptyList(), AlbumSort.DEFAULT))

        composeRule.onNode(hasText(string(R.string.feature_albums_empty))).assertExists()
    }

    @Test
    fun unreachableServer_showsTheErrorAndTriesAgain() {
        var reloads = 0
        showContent(uiState = AlbumsUiState.Failed(LibraryFailure.UNREACHABLE, AlbumSort.DEFAULT), onReload = {
            reloads++
        })

        composeRule.onNode(hasText(string(R.string.feature_albums_error_unreachable))).assertExists()
        composeRule.onNode(hasText(string(R.string.feature_albums_retry)) and hasClickAction()).performClick()

        assertEquals(1, reloads)
    }

    @Test
    fun failedRefresh_showsTheErrorOverTheAlbums() {
        var shown = false
        showContent(refreshFailure = LibraryFailure.SERVER_ERROR, onRefreshFailureShown = { shown = true })

        composeRule.onNode(hasText(string(R.string.feature_albums_error_server))).assertExists()
        composeRule.onNode(hasText("Morning Light")).assertExists()
        // The snackbar is shown until it times out; the screen only hears back once it's gone.
        composeRule.waitUntil(timeoutMillis = 10_000) { shown }
    }

    // The grid keeps its first visible album in place when albums are added before it, which would
    // leave the albums a refresh adds at the top hidden above it.
    @Test
    fun refreshAddingAlbumsAtTheTop_showsThem() {
        val newestFirst = AlbumSort(AlbumSortField.DATE_ADDED, SortDirection.DESCENDING)
        val olderAlbums = (1..20).map(::album)
        val shownState = showContent(uiState = AlbumsUiState.Loaded(olderAlbums, newestFirst))

        // Three, so the previous first album moves to the second line whether the grid has two or three columns.
        val newAlbums = (21..23).map(::album)
        shownState.value = AlbumsUiState.Loaded(newAlbums + olderAlbums, newestFirst)

        composeRule.onNode(hasText("Album 21")).assertIsDisplayed()
    }

    private fun menuItem(label: Int): SemanticsMatcher = hasText(string(label)) and hasClickAction()

    private fun isSelected(selected: Boolean): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Selected, selected)

    private fun showContent(
        uiState: AlbumsUiState = AlbumsUiState.Loaded(albums, AlbumSort.DEFAULT),
        refreshFailure: LibraryFailure? = null,
        onSelectSort: (AlbumSortField) -> Unit = {},
        onReload: () -> Unit = {},
        onRefreshFailureShown: () -> Unit = {},
        onAlbumClick: (String) -> Unit = {}
    ): MutableState<AlbumsUiState> {
        val shownState = mutableStateOf(uiState)
        composeRule.setContent {
            JellyfinMusicTheme {
                AlbumsContent(
                    uiState = shownState.value,
                    isRefreshing = false,
                    refreshFailure = refreshFailure,
                    onSelectSort = onSelectSort,
                    onReload = onReload,
                    onRefreshFailureShown = onRefreshFailureShown,
                    onAlbumClick = onAlbumClick
                )
            }
        }
        // Changing it shows the new state, as a refresh does.
        return shownState
    }

    private fun album(number: Int) =
        Album(id = "$number", name = "Album $number", artistName = "Artist $number", coverImageUrl = null)

    private fun string(resource: Int): String = composeRule.activity.getString(resource)
}
