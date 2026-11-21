/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.model

import com.vitorpamplona.amethyst.commons.model.HomeFeedType
import com.vitorpamplona.quartz.nip10Notes.TextNoteEvent
import com.vitorpamplona.quartz.nip35Torrents.TorrentCommentEvent
import com.vitorpamplona.quartz.nip35Torrents.TorrentEvent
import com.vitorpamplona.quartz.nip68Picture.PictureEvent
import com.vitorpamplona.quartz.nip71Video.AddressableNormalVideoEvent
import com.vitorpamplona.quartz.nip71Video.VideoNormalEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedTypeTest {
    @Test
    fun allContainsEveryEntry() {
        assertEquals(HomeFeedType.entries.toSet(), HomeFeedType.ALL)
    }

    @Test
    fun kindsAreDisjointAcrossTypes() {
        val seen = mutableSetOf<Int>()
        HomeFeedType.entries.forEach { type ->
            type.kinds.forEach { kind ->
                assertTrue("kind $kind is owned by more than one HomeFeedType", seen.add(kind))
            }
        }
    }

    @Test
    fun encodeThenDecodeRoundTrips() {
        val disabled = setOf(HomeFeedType.CHESS, HomeFeedType.BIRDS)
        val enabled = HomeFeedType.ALL - disabled
        val stored = HomeFeedType.encode(HomeFeedType.ALL - enabled)
        assertEquals(enabled, HomeFeedType.ALL - HomeFeedType.decode(stored))
    }

    @Test
    fun decodeNullOrBlankIsEmpty() {
        assertEquals(emptySet<HomeFeedType>(), HomeFeedType.decode(null))
        // Absence of a stored value means "nothing disabled" -> everything enabled.
        assertEquals(HomeFeedType.ALL, HomeFeedType.ALL - HomeFeedType.decode(null))
    }

    @Test
    fun decodeDropsUnknownCodes() {
        val decoded = HomeFeedType.decode("chess,future-kind")
        assertEquals(setOf(HomeFeedType.CHESS), decoded)
        assertNull(HomeFeedType.fromCode("future-kind"))
    }

    @Suppress("DEPRECATION") // TorrentCommentEvent: replaced by NIP-22, still part of the Torrents feed
    @Test
    fun picturesVideosAndTorrentsOwnTheirKinds() {
        assertEquals(listOf(PictureEvent.KIND), HomeFeedType.PICTURES.kinds)
        // Home carries the horizontal/normal kinds only; the vertical/short ones are Reels'.
        assertEquals(listOf(VideoNormalEvent.KIND, AddressableNormalVideoEvent.KIND), HomeFeedType.VIDEOS.kinds)
        // Torrent comments ride the Torrents toggle, so turning it off hides the whole conversation.
        assertEquals(listOf(TorrentEvent.KIND, TorrentCommentEvent.KIND), HomeFeedType.TORRENTS.kinds)
    }

    @Test
    fun disablingVideosLeavesPicturesUntouched() {
        val disabled = HomeFeedType.disabledKinds(HomeFeedType.ALL - HomeFeedType.VIDEOS)
        HomeFeedType.VIDEOS.kinds.forEach { assertTrue(it in disabled) }
        assertFalse(PictureEvent.KIND in disabled)
    }

    @Test
    fun defaultHomeIsMediaOnly() {
        assertEquals(
            setOf(HomeFeedType.PICTURES, HomeFeedType.VIDEOS, HomeFeedType.VOICE, HomeFeedType.MUSIC),
            HomeFeedType.DEFAULT_ENABLED,
        )
        // Text, reposts and every niche group are opt-in: the user turns them on in settings.
        val disabled = HomeFeedType.disabledKinds(HomeFeedType.DEFAULT_ENABLED)
        assertTrue(TextNoteEvent.KIND in disabled)
        assertTrue(HomeFeedType.REPOSTS.kinds.all { it in disabled })
        assertFalse(PictureEvent.KIND in disabled)
        assertFalse(VideoNormalEvent.KIND in disabled)
        assertFalse(AddressableNormalVideoEvent.KIND in disabled)
    }

    @Test
    fun shortsNeverReachHome() {
        // Reels owns kinds 22 and 34236: Home drops them even with every toggle on.
        HomeFeedType.REELS_ONLY_KINDS.forEach {
            assertTrue("$it is still live in Home", it in HomeFeedType.disabledKinds(HomeFeedType.ALL))
            assertTrue("$it is still live in Home", it in HomeFeedType.disabledKinds(HomeFeedType.DEFAULT_ENABLED))
        }
        assertNull(HomeFeedType.fromCode("shorts"))
    }

    @Test
    fun disabledKindsHoldsOnlyTheReelsKindsWhenEverythingEnabled() {
        assertEquals(HomeFeedType.REELS_ONLY_KINDS, HomeFeedType.disabledKinds(HomeFeedType.ALL))
    }

    @Test
    fun disabledKindsAreExactlyTheDisabledGroupsKinds() {
        val enabled = HomeFeedType.ALL - HomeFeedType.TEXT_NOTES - HomeFeedType.REPOSTS
        val disabled = HomeFeedType.disabledKinds(enabled)

        assertTrue(TextNoteEvent.KIND in disabled)
        HomeFeedType.REPOSTS.kinds.forEach { assertTrue(it in disabled) }
        // A still-enabled group's kinds must not leak into the disabled set.
        HomeFeedType.POLLS.kinds.forEach { assertFalse(it in disabled) }
    }
}
