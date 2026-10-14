package com.example.nplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTreeTest {

    @Test
    fun `relative paths are trimmed to a bare folder`() {
        assertEquals("Movies/2024", normalizeFolder("Movies/2024/"))
        assertEquals("Movies", normalizeFolder("/Movies/"))
        assertEquals("", normalizeFolder(null))
        assertEquals("", normalizeFolder("   "))
        assertEquals("A/B", normalizeFolder("A//B//"))
    }

    @Test
    fun `absolute paths lose their storage root`() {
        assertEquals("Movies", stripStorageRoot("/storage/emulated/0/Movies"))
        assertEquals("Movies/2024", stripStorageRoot("/storage/emulated/0/Movies/2024/"))
        assertEquals("Music", stripStorageRoot("/storage/1234-5678/Music"))
        assertEquals("Music", stripStorageRoot("/sdcard/Music"))
        assertEquals("DCIM", stripStorageRoot("/storage/self/primary/DCIM"))
    }

    @Test
    fun `parent walks one level and stops at the root`() {
        assertEquals("Movies", parentFolder("Movies/2024"))
        assertEquals("", parentFolder("Movies"))
        assertNull(parentFolder(""))
    }

    @Test
    fun `name is the last segment, or the root label`() {
        assertEquals("2024", folderName("Movies/2024"))
        assertEquals("Movies", folderName("Movies"))
        assertEquals(ROOT_FOLDER_LABEL, folderName(""))
    }

    @Test
    fun `root lists only the first level of folders`() {
        val children = childFolders(
            folder = "",
            itemFolders = listOf("Movies", "Movies/2024", "Music", "DCIM/Camera"),
        )
        assertEquals(listOf("DCIM", "Movies", "Music"), children.map { it.name })
    }

    @Test
    fun `child counts are recursive`() {
        // itemFolders holds containing folders, so two files in Movies/2024 are
        // "Movies/2024" twice.
        val children = childFolders(
            folder = "",
            itemFolders = listOf("Movies", "Movies/2024", "Movies/2024", "Music"),
        )
        assertEquals(3, children.first { it.name == "Movies" }.itemCount)
        assertEquals(1, children.first { it.name == "Music" }.itemCount)
    }

    @Test
    fun `inside a folder the listing is its own children`() {
        val children = childFolders(
            folder = "Movies",
            itemFolders = listOf("Movies", "Movies/2024", "Movies/2024", "Music"),
        )
        assertEquals(listOf("2024"), children.map { it.name })
        assertEquals(2, children.first().itemCount)
    }

    @Test
    fun `folders with no items are omitted`() {
        // A folder only appears when something inside it was found.
        val children = childFolders("", itemFolders = listOf("Movies"))
        assertEquals(listOf("Movies"), children.map { it.name })
    }

    @Test
    fun `a file in the current folder creates no child`() {
        val children = childFolders("Movies", itemFolders = listOf("Movies"))
        assertTrue(children.isEmpty())
    }

    @Test
    fun `items outside the current folder are ignored`() {
        val children = childFolders("Movies", itemFolders = listOf("Movies/2024", "Music"))
        assertEquals(listOf("2024"), children.map { it.name })
        assertEquals(1, children.first().itemCount)
    }

    @Test
    fun `root detection`() {
        assertTrue(isRootFolder(""))
        assertTrue(isRootFolder("/"))
        assertFalse(isRootFolder("Movies"))
    }

    @Test
    fun `breadcrumbs start at the root and walk down`() {
        val crumbs = breadcrumbsFor("Movies/2024")
        assertEquals(listOf(ROOT_FOLDER_LABEL, "Movies", "2024"), crumbs.map { it.label })
        assertEquals(listOf("", "Movies", "Movies/2024"), crumbs.map { it.path })
    }

    @Test
    fun `breadcrumbs at the root are just the root`() {
        val crumbs = breadcrumbsFor("")
        assertEquals(1, crumbs.size)
        assertEquals(ROOT_FOLDER_LABEL, crumbs.first().label)
        assertEquals("", crumbs.first().path)
    }
}
