package io.github.lorber13.jellyfinmusic.core.data.library

import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumSortRequestTest {
    @Test
    fun name_sortsBySortNameInTheChosenDirection() {
        val sort = AlbumSort(AlbumSortField.NAME, SortDirection.DESCENDING)

        assertEquals(listOf(ItemSortBy.SORT_NAME), sort.toSortBy())
        assertEquals(listOf(SortOrder.DESCENDING), sort.toSortOrder())
    }

    @Test
    fun year_sortsByYearThenNameFromA() {
        val sort = AlbumSort(AlbumSortField.YEAR, SortDirection.DESCENDING)

        assertEquals(listOf(ItemSortBy.PRODUCTION_YEAR, ItemSortBy.SORT_NAME), sort.toSortBy())
        assertEquals(listOf(SortOrder.DESCENDING, SortOrder.ASCENDING), sort.toSortOrder())
    }

    @Test
    fun dateAdded_sortsByDateCreatedThenNameFromA() {
        val sort = AlbumSort(AlbumSortField.DATE_ADDED, SortDirection.ASCENDING)

        assertEquals(listOf(ItemSortBy.DATE_CREATED, ItemSortBy.SORT_NAME), sort.toSortBy())
        assertEquals(listOf(SortOrder.ASCENDING, SortOrder.ASCENDING), sort.toSortOrder())
    }

    @Test
    fun eachField_startsInItsNaturalDirection() {
        assertEquals(SortDirection.ASCENDING, AlbumSortField.NAME.naturalDirection)
        assertEquals(SortDirection.DESCENDING, AlbumSortField.YEAR.naturalDirection)
        assertEquals(SortDirection.DESCENDING, AlbumSortField.DATE_ADDED.naturalDirection)
    }
}
