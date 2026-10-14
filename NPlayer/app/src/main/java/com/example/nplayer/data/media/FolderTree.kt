package com.example.nplayer.data.media

/** A folder in the device tree, with a recursive count of the items beneath it. */
data class FolderSummary(
    val path: String,
    val name: String,
    val itemCount: Int,
)

/** The label shown for the storage root, where items have no containing folder. */
const val ROOT_FOLDER_LABEL = "All files"

/**
 * Trims a MediaStore `RELATIVE_PATH` (which ends in a slash, e.g. `"Movies/2024/"`)
 * down to a bare folder path (`"Movies/2024"`). Blank or null becomes the root.
 */
fun normalizeFolder(raw: String?): String =
    raw.orEmpty().trim().trim('/').replace(Regex("/{2,}"), "/")

/**
 * Strips a storage root from an absolute directory path, as returned by the legacy
 * `DATA` column: `/storage/emulated/0/Movies` and `/storage/1234-5678/Movies` both
 * become `Movies`, so internal and SD-card storage share one tree.
 */
fun stripStorageRoot(path: String): String {
    val trimmed = path.trim().trim('/')
    for (root in listOf("storage/emulated/0/", "storage/self/primary/", "sdcard/")) {
        if (trimmed.startsWith(root)) return trimmed.removePrefix(root)
    }
    Regex("^storage/[^/]+/").find(trimmed)?.let { return trimmed.removePrefix(it.value) }
    return trimmed
}

/** The immediate parent of [folder], or null when [folder] is the root. */
fun parentFolder(folder: String): String? {
    val normalized = normalizeFolder(folder)
    if (normalized.isEmpty()) return null
    return normalized.substringBeforeLast('/', "")
}

/** The last path segment of [folder], for display. The root has no segment. */
fun folderName(folder: String): String {
    val normalized = normalizeFolder(folder)
    return if (normalized.isEmpty()) ROOT_FOLDER_LABEL else normalized.substringAfterLast('/')
}

/**
 * The immediate child folders of [folder] that hold at least one item somewhere
 * beneath them, each with a recursive item count, sorted by name.
 *
 * An item is "beneath" a child when its folder equals the child or starts with the
 * child plus a slash, which is what makes the counts recursive rather than direct.
 */
fun childFolders(folder: String, itemFolders: List<String>): List<FolderSummary> {
    val base = normalizeFolder(folder)
    val childDepth = if (base.isEmpty()) 1 else base.split('/').size + 1
    val counts = LinkedHashMap<String, Int>()

    for (raw in itemFolders) {
        val itemFolder = normalizeFolder(raw)
        if (itemFolder.isEmpty()) continue
        if (base.isNotEmpty() && itemFolder != base && !itemFolder.startsWith("$base/")) continue
        val segments = itemFolder.split('/')
        if (segments.size < childDepth) continue
        val childPath = segments.take(childDepth).joinToString("/")
        counts[childPath] = (counts[childPath] ?: 0) + 1
    }

    return counts
        .map { (path, count) -> FolderSummary(path, folderName(path), count) }
        .sortedBy { it.name.lowercase() }
}

/** True when [folder] is the root, so the caller can show the root label. */
fun isRootFolder(folder: String): Boolean = normalizeFolder(folder).isEmpty()

/** One step in the folder path, ready to render as a clickable crumb. */
data class FolderCrumb(val label: String, val path: String)

/** The trail from the root down to [folder], always starting with the root crumb. */
fun breadcrumbsFor(folder: String): List<FolderCrumb> {
    val crumbs = mutableListOf(FolderCrumb(ROOT_FOLDER_LABEL, ""))
    var path = ""
    for (segment in normalizeFolder(folder).split('/')) {
        if (segment.isEmpty()) continue
        path = if (path.isEmpty()) segment else "$path/$segment"
        crumbs += FolderCrumb(segment, path)
    }
    return crumbs
}
