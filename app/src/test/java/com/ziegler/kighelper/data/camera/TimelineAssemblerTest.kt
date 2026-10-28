package com.ziegler.kighelper.data.camera

import com.ziegler.kighelper.data.scan.ScanRecord
import com.ziegler.kighelper.data.scan.ScanRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineAssemblerTest {

    private fun scan(id: String, timestamp: Long) = ScanRecord(
        id = id,
        timestamp = timestamp,
        type = ScanRecordType.TEXT,
        rawContent = id,
        title = id
    )

    @Test
    fun mergesScansInDescendingTimeOrder() {
        val items = TimelineAssembler.merge(
            photos = emptyList(),
            scans = listOf(scan("old", 100), scan("new", 300), scan("middle", 200))
        )

        assertEquals(
            listOf("new", "middle", "old"),
            items.map { (it as TimelineItem.Scan).record.id }
        )
    }

    @Test
    fun filtersTimelineTypes() {
        val scans = listOf(scan("scan", 100))

        assertTrue(
            TimelineAssembler.merge(emptyList(), scans, TimelineFilter.PHOTOS).isEmpty()
        )
        assertEquals(
            1,
            TimelineAssembler.merge(emptyList(), scans, TimelineFilter.SCANS).size
        )
    }

    @Test
    fun selectionTracksBothKindsAndCounts() {
        val items = listOf(
            TimelineItem.Scan(scan("a", 100)),
            TimelineItem.Scan(scan("b", 200))
        )
        val selected = TimelineSelection().selectAll(items)

        assertEquals(2, selected.scanCount)
        assertEquals(0, selected.photoCount)
        assertEquals(2, selected.totalCount)

        val toggled = selected.toggle(items.first())
        assertEquals(1, toggled.scanCount)
        assertEquals(1, toggled.totalCount)
    }
}
