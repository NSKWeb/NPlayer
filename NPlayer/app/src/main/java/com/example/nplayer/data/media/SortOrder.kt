package com.example.nplayer.data.media

/** How the media lists (Home, Folders, Search) arrange their files. */
enum class FileSortOrder(val label: String) {
    DATE_NEWEST("Newest first"),
    DATE_OLDEST("Oldest first"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    SIZE_LARGEST("Largest first"),
    DURATION_LONGEST("Longest first"),
    ;

    companion object {
        fun fromStorage(raw: String?): FileSortOrder =
            entries.firstOrNull { it.name == raw } ?: DATE_NEWEST
    }
}

/** How the folder list arranges its sub-folders. */
enum class FolderSortOrder(val label: String) {
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    ITEM_COUNT("Most items"),
    ;

    companion object {
        fun fromStorage(raw: String?): FolderSortOrder =
            entries.firstOrNull { it.name == raw } ?: NAME_ASC
    }
}

/** The comparator for [order], applied to a media list. */
fun FileSortOrder.comparator(): Comparator<MediaItem> = when (this) {
    FileSortOrder.DATE_NEWEST -> compareByDescending { it.dateAddedSeconds }
    FileSortOrder.DATE_OLDEST -> compareBy { it.dateAddedSeconds }
    FileSortOrder.NAME_ASC -> compareBy { it.title.lowercase() }
    FileSortOrder.NAME_DESC -> compareByDescending { it.title.lowercase() }
    FileSortOrder.SIZE_LARGEST -> compareByDescending { it.sizeBytes }
    FileSortOrder.DURATION_LONGEST -> compareByDescending { it.durationMillis }
}

/** The comparator for [order], applied to a folder list. */
fun FolderSortOrder.comparator(): Comparator<FolderSummary> = when (this) {
    FolderSortOrder.NAME_ASC -> compareBy { it.name.lowercase() }
    FolderSortOrder.NAME_DESC -> compareByDescending { it.name.lowercase() }
    FolderSortOrder.ITEM_COUNT -> compareByDescending<FolderSummary> { it.itemCount }
        .thenBy { it.name.lowercase() }
}
