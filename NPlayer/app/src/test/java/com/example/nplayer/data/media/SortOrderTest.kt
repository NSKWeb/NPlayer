package com.example.nplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SortOrderTest {

    private fun item(
        title: String,
        dateAddedSeconds: Long = 0,
        sizeBytes: Long = 0,
        durationMillis: Long = 0,
    ) = MediaItem(
        id = title.hashCode().toLong(),
        uri = android.net.Uri.parse("content://media/$title"),
        title = title,
        kind = MediaKind.VIDEO,
        durationMillis = durationMillis,
        sizeBytes = sizeBytes,
        dateAddedSeconds = dateAddedSeconds,
    )

    private fun sorted(order: FileSortOrder, items: List<MediaItem>) =
        items.sortedWith(order.comparator()).map { it.title }

    @Test
    fun `date order runs newest and oldest`() {
        val a = item("a", dateAddedSeconds = 100)
        val b = item("b", dateAddedSeconds = 300)
        val c = item("c", dateAddedSeconds = 200)

        assertEquals(listOf("b", "c", "a"), sorted(FileSortOrder.DATE_NEWEST, listOf(a, b, c)))
        assertEquals(listOf("a", "c", "b"), sorted(FileSortOrder.DATE_OLDEST, listOf(a, b, c)))
    }

    @Test
    fun `name order is case insensitive`() {
        val lower = item("apple")
        val upper = item("Banana")
        val cherry = item("cherry")

        assertEquals(
            listOf("apple", "Banana", "cherry"),
            sorted(FileSortOrder.NAME_ASC, listOf(cherry, upper, lower)),
        )
        assertEquals(
            listOf("cherry", "Banana", "apple"),
            sorted(FileSortOrder.NAME_DESC, listOf(lower, upper, cherry)),
        )
    }

    @Test
    fun `size and duration order put the largest and longest first`() {
        val small = item("small", sizeBytes = 10, durationMillis = 10)
        val big = item("big", sizeBytes = 900, durationMillis = 900)
        val mid = item("mid", sizeBytes = 500, durationMillis = 500)

        assertEquals(
            listOf("big", "mid", "small"),
            sorted(FileSortOrder.SIZE_LARGEST, listOf(small, big, mid)),
        )
        assertEquals(
            listOf("big", "mid", "small"),
            sorted(FileSortOrder.DURATION_LONGEST, listOf(small, big, mid)),
        )
    }

    @Test
    fun `unknown stored values fall back to the defaults`() {
        assertEquals(FileSortOrder.DATE_NEWEST, FileSortOrder.fromStorage("nonsense"))
        assertEquals(FileSortOrder.DATE_NEWEST, FileSortOrder.fromStorage(null))
        assertEquals(FolderSortOrder.NAME_ASC, FolderSortOrder.fromStorage("nonsense"))
        assertEquals(FolderSortOrder.NAME_ASC, FolderSortOrder.fromStorage(null))
    }

    @Test
    fun `stored values round trip`() {
        for (order in FileSortOrder.entries) {
            assertEquals(order, FileSortOrder.fromStorage(order.name))
        }
        for (order in FolderSortOrder.entries) {
            assertEquals(order, FolderSortOrder.fromStorage(order.name))
        }
    }

    @Test
    fun `folder item count order breaks ties by name`() {
        val many = FolderSummary("a", "Zeta", 5)
        val few = FolderSummary("b", "Alpha", 2)
        val alsoMany = FolderSummary("c", "Beta", 5)

        val result = listOf(many, few, alsoMany)
            .sortedWith(FolderSortOrder.ITEM_COUNT.comparator())
            .map { it.name }

        assertEquals(listOf("Beta", "Zeta", "Alpha"), result)
    }
}
