package com.ziegler.kighelper.ui.components

import com.ziegler.kighelper.data.SocialCardProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialCardEntryTest {

    @Test
    fun aggregateIsVisibleByDefault() {
        val entries = socialCardEntries(SocialCardProfile(nickname = "测试昵称"))

        assertTrue(entries.first() is SocialCardEntry.Aggregate)
    }

    @Test
    fun aggregateIsHiddenWhenDisabled() {
        val entries = socialCardEntries(
            SocialCardProfile(nickname = "测试昵称", showAggregate = false)
        )

        assertEquals(emptyList<SocialCardEntry>(), entries)
    }
}
