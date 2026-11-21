package io.github.lorber13.jellyfinmusic.core.data.library

/** The order of the albums list. */
data class AlbumSort(val field: AlbumSortField, val direction: SortDirection) {
    companion object {
        val DEFAULT = AlbumSort(AlbumSortField.NAME, SortDirection.ASCENDING)
    }
}

/**
 * What the albums list is sorted by.
 *
 * @param naturalDirection the direction users expect first: names from A, years and dates from the newest.
 */
enum class AlbumSortField(val naturalDirection: SortDirection) {
    NAME(SortDirection.ASCENDING),
    YEAR(SortDirection.DESCENDING),
    DATE_ADDED(SortDirection.DESCENDING)
}

enum class SortDirection {
    ASCENDING,
    DESCENDING;

    fun reversed(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}
