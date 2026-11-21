package io.github.lorber13.jellyfinmusic.core.data.library

import io.github.lorber13.jellyfinmusic.core.data.di.IoDispatcher
import io.github.lorber13.jellyfinmusic.core.data.session.JellyfinSessionRepository
import io.github.lorber13.jellyfinmusic.core.data.session.SessionState
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.microseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.exception.InvalidContentException
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest

/**
 * Cover size in pixels: a grid cell is about 180 dp wide, which is under 512 px at a phone's
 * density. The server scales the image down, so smaller downloads.
 */
private const val COVER_SIZE_PIXELS = 512

/** Cover size in pixels for the album screen's header, which is about 280 dp wide (840 px on a Pixel). */
private const val LARGE_COVER_SIZE_PIXELS = 1024

/** Jellyfin durations are in ticks of 100 nanoseconds. */
private const val TICKS_PER_MICROSECOND = 10

/**
 * Loads the albums from the signed-in user's server. All albums come in one request; libraries
 * of 10,000+ albums are LIB-12. Public, with an internal constructor, because the public
 * `LibraryModule` binds it.
 */
class JellyfinAlbumRepository @Inject internal constructor(
    private val sessionRepository: JellyfinSessionRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AlbumRepository {
    override suspend fun getAlbums(sort: AlbumSort): AlbumsResult {
        val (client, userId) = signedInClientAndUserId()
        val request = GetItemsRequest(
            userId = userId,
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            recursive = true,
            sortBy = sort.toSortBy(),
            sortOrder = sort.toSortOrder(),
            // Only what the list shows: no play counts or favorites, and only the cover image.
            enableUserData = false,
            enableImageTypes = listOf(ImageType.PRIMARY),
            imageTypeLimit = 1,
            enableTotalRecordCount = false
        )
        val items = try {
            // The SDK reads responses on the calling thread.
            withContext(ioDispatcher) { client.libraryApi.getItems(request).content.items }
        } catch (error: ApiClientException) {
            return AlbumsResult.Failed(libraryFailureOf(error))
        }
        return AlbumsResult.Success(
            items.map { item ->
                albumOf(item, coverImageUrlOf(client, item, COVER_SIZE_PIXELS))
            }
        )
    }

    override suspend fun getAlbumDetail(albumId: String): AlbumDetailResult {
        val (client, userId) = signedInClientAndUserId()
        val albumUuid = UUID.fromString(albumId)
        val tracksRequest = GetItemsRequest(
            userId = userId,
            parentId = albumUuid,
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER, ItemSortBy.SORT_NAME),
            // The album screen shows no track images or play counts.
            enableUserData = false,
            enableImages = false,
            enableTotalRecordCount = false
        )
        return try {
            // The SDK reads responses on the calling thread. Both requests run at the same time.
            withContext(ioDispatcher) {
                val album = async { client.libraryApi.getItem(itemId = albumUuid, userId = userId).content }
                val tracks = async { client.libraryApi.getItems(tracksRequest).content.items }
                val albumItem = album.await()
                AlbumDetailResult.Success(
                    albumDetailOf(
                        item = albumItem,
                        coverImageUrl = coverImageUrlOf(client, albumItem, LARGE_COVER_SIZE_PIXELS),
                        tracks = tracks.await().map(::trackOf)
                    )
                )
            }
        } catch (error: ApiClientException) {
            AlbumDetailResult.Failed(libraryFailureOf(error))
        }
    }

    private fun signedInClientAndUserId(): Pair<ApiClient, UUID> {
        val client = sessionRepository.signedInClient
        val session = sessionRepository.session.value
        // The library screens are only shown while someone is signed in.
        check(client != null && session is SessionState.SignedIn) { "Library requested with nobody signed in" }
        return client to UUID.fromString(session.session.userId)
    }
}

/** The server's sort keys for [AlbumSort]. Albums with the same year or date are ordered by name. */
internal fun AlbumSort.toSortBy(): List<ItemSortBy> = when (field) {
    AlbumSortField.NAME -> listOf(ItemSortBy.SORT_NAME)
    AlbumSortField.YEAR -> listOf(ItemSortBy.PRODUCTION_YEAR, ItemSortBy.SORT_NAME)
    AlbumSortField.DATE_ADDED -> listOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME)
}

/** One order per key of [toSortBy]: the chosen direction, then names always from A. */
internal fun AlbumSort.toSortOrder(): List<SortOrder> {
    val order = if (direction == SortDirection.ASCENDING) SortOrder.ASCENDING else SortOrder.DESCENDING
    return if (field == AlbumSortField.NAME) listOf(order) else listOf(order, SortOrder.ASCENDING)
}

internal fun albumOf(item: BaseItemDto, coverImageUrl: String?): Album = Album(
    id = item.id.toString(),
    name = item.name.orEmpty(),
    artistName = albumArtistOf(item),
    coverImageUrl = coverImageUrl
)

internal fun albumDetailOf(item: BaseItemDto, coverImageUrl: String?, tracks: List<Track>): AlbumDetail = AlbumDetail(
    id = item.id.toString(),
    name = item.name.orEmpty(),
    artistName = albumArtistOf(item),
    year = item.productionYear,
    coverImageUrl = coverImageUrl,
    tracks = tracks
)

internal fun trackOf(item: BaseItemDto): Track = Track(
    id = item.id.toString(),
    name = item.name.orEmpty(),
    artistNames = item.artists.orEmpty().filter { it.isNotBlank() },
    trackNumber = item.indexNumber,
    discNumber = item.parentIndexNumber,
    duration = item.runTimeTicks?.let { ticks -> (ticks / TICKS_PER_MICROSECOND).microseconds }
)

// AlbumArtist holds the album artists joined into one name; the list is a fallback for servers that leave it out.
private fun albumArtistOf(item: BaseItemDto): String? = item.albumArtist?.takeIf { it.isNotBlank() }
    ?: item.albumArtists?.firstOrNull()?.name?.takeIf { it.isNotBlank() }

/** The cover's address, or null without artwork. The tag changes with the artwork, so cached covers stay current. */
private fun coverImageUrlOf(client: ApiClient, item: BaseItemDto, sizePixels: Int): String? {
    val tag = item.imageTags?.get(ImageType.PRIMARY) ?: return null
    return client.imageApi.getItemImageUrl(
        itemId = item.id,
        imageType = ImageType.PRIMARY,
        maxWidth = sizePixels,
        maxHeight = sizePixels,
        tag = tag
    )
}

internal fun libraryFailureOf(error: ApiClientException): LibraryFailure = when (error) {
    // A 401 means the session expired, which gets its own handling in ACC-06.
    is InvalidStatusException, is InvalidContentException -> LibraryFailure.SERVER_ERROR

    // Timeouts, refused connections, certificate problems and blocked local network access.
    else -> LibraryFailure.UNREACHABLE
}
