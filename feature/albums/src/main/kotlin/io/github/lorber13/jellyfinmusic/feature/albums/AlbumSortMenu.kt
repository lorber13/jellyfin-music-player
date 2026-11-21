package io.github.lorber13.jellyfinmusic.feature.albums

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSort
import io.github.lorber13.jellyfinmusic.core.data.library.AlbumSortField
import io.github.lorber13.jellyfinmusic.core.data.library.SortDirection

/**
 * The sort button of the albums screen's title bar and its menu. The selected option has a check
 * and an arrow for its direction; picking it again reverses the direction.
 */
@Composable
internal fun AlbumSortMenu(sort: AlbumSort, onSelectSort: (AlbumSortField) -> Unit, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    // The menu opens next to the button because they share this Box.
    Box(modifier = modifier) {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                painter = painterResource(R.drawable.feature_albums_ic_sort),
                contentDescription = stringResource(R.string.feature_albums_sort)
            )
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            AlbumSortField.entries.forEach { field ->
                SortOption(
                    field = field,
                    selectedSort = sort,
                    onClick = {
                        isExpanded = false
                        onSelectSort(field)
                    }
                )
            }
        }
    }
}

@Composable
private fun SortOption(field: AlbumSortField, selectedSort: AlbumSort, onClick: () -> Unit) {
    val isSelected = field == selectedSort.field
    DropdownMenuItem(
        text = { Text(stringResource(labelOf(field))) },
        onClick = onClick,
        // Screen readers announce "selected"; the check itself is only visual.
        modifier = Modifier.semantics { selected = isSelected },
        leadingIcon = {
            if (isSelected) {
                Icon(painter = painterResource(R.drawable.feature_albums_ic_check), contentDescription = null)
            } else {
                // Keeps the labels of all options aligned.
                Spacer(Modifier.size(24.dp))
            }
        },
        trailingIcon = if (isSelected) {
            { SortDirectionIcon(selectedSort) }
        } else {
            null
        }
    )
}

@Composable
private fun SortDirectionIcon(sort: AlbumSort) {
    val iconResource = when (sort.direction) {
        SortDirection.ASCENDING -> R.drawable.feature_albums_ic_arrow_upward
        SortDirection.DESCENDING -> R.drawable.feature_albums_ic_arrow_downward
    }
    Icon(painter = painterResource(iconResource), contentDescription = stringResource(directionDescriptionOf(sort)))
}

@StringRes
private fun labelOf(field: AlbumSortField): Int = when (field) {
    AlbumSortField.NAME -> R.string.feature_albums_sort_name
    AlbumSortField.YEAR -> R.string.feature_albums_sort_year
    AlbumSortField.DATE_ADDED -> R.string.feature_albums_sort_date_added
}

/** "Ascending" means different things to people for names and for dates, so each gets its own words. */
@StringRes
private fun directionDescriptionOf(sort: AlbumSort): Int {
    val isAscending = sort.direction == SortDirection.ASCENDING
    return when (sort.field) {
        AlbumSortField.NAME ->
            if (isAscending) R.string.feature_albums_sort_a_to_z else R.string.feature_albums_sort_z_to_a

        AlbumSortField.YEAR, AlbumSortField.DATE_ADDED ->
            if (isAscending) R.string.feature_albums_sort_oldest_first else R.string.feature_albums_sort_newest_first
    }
}
