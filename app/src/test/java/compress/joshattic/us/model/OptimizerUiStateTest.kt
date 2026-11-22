package compress.joshattic.us.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OptimizerUiStateTest {

    @Test
    fun estimatedReclaim_sumsSelectedSavings() {
        val items = listOf(
            MediaLibraryItem(
                id = 1,
                uri = Uri.parse("content://media/1"),
                kind = MediaKind.VIDEO,
                displayName = "a.mp4",
                sizeBytes = 1000,
                estimatedSavingsBytes = 400
            ),
            MediaLibraryItem(
                id = 2,
                uri = Uri.parse("content://media/2"),
                kind = MediaKind.IMAGE,
                displayName = "b.jpg",
                sizeBytes = 500,
                estimatedSavingsBytes = 150
            )
        )
        val state = OptimizerUiState(
            allItems = items,
            selectedIds = setOf(1, 2)
        )
        assertEquals(550L, state.estimatedReclaimBytes)
    }

    @Test
    fun items_hidesExcludedFileAndFolder() {
        val items = listOf(
            MediaLibraryItem(
                id = 1,
                uri = Uri.parse("content://media/1"),
                kind = MediaKind.VIDEO,
                displayName = "a.mp4",
                sizeBytes = 1000,
                folderKey = "DCIM/Camera",
                folderLabel = "Camera"
            ),
            MediaLibraryItem(
                id = 2,
                uri = Uri.parse("content://media/2"),
                kind = MediaKind.VIDEO,
                displayName = "b.mp4",
                sizeBytes = 800,
                folderKey = "DCIM/Camera",
                folderLabel = "Camera"
            ),
            MediaLibraryItem(
                id = 3,
                uri = Uri.parse("content://media/3"),
                kind = MediaKind.IMAGE,
                displayName = "c.jpg",
                sizeBytes = 400,
                folderKey = "Download",
                folderLabel = "Download"
            )
        )
        val hiddenFile = OptimizerUiState(allItems = items, excludedIds = setOf(1))
        assertEquals(listOf(2L, 3L), hiddenFile.items.map { it.id })

        val hiddenFolder = OptimizerUiState(allItems = items, excludedFolders = setOf("DCIM/Camera"))
        assertEquals(listOf(3L), hiddenFolder.items.map { it.id })
        assertEquals(true, hiddenFolder.hasExclusions)
    }

    @Test
    fun reviewingItem_resolvesById() {
        val media = MediaLibraryItem(
            id = 9,
            uri = Uri.parse("content://media/9"),
            kind = MediaKind.VIDEO,
            displayName = "clip.mp4",
            sizeBytes = 2000
        )
        val queueItem = OptimizerQueueItem(id = "q1", media = media, status = QueueItemStatus.DONE)
        val state = OptimizerUiState(queue = listOf(queueItem), reviewingItemId = "q1")
        assertEquals("q1", state.reviewingItem?.id)
        assertNull(state.copy(reviewingItemId = null).reviewingItem)
    }

    @Test
    fun queueItem_savedBytes_onlyWhenSmaller() {
        val media = MediaLibraryItem(
            id = 1,
            uri = Uri.parse("content://media/1"),
            kind = MediaKind.IMAGE,
            displayName = "pic.jpg",
            sizeBytes = 1000
        )
        val done = OptimizerQueueItem(
            id = "1",
            media = media,
            status = QueueItemStatus.DONE,
            outputSizeBytes = 600
        )
        assertEquals(400L, done.savedBytes)

        val larger = done.copy(outputSizeBytes = 1200)
        assertEquals(0L, larger.savedBytes)
    }
}
