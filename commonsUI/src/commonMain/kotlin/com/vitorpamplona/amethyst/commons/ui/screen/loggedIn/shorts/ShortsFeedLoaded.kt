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
package com.vitorpamplona.amethyst.commons.ui.screen.loggedIn.shorts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitorpamplona.amethyst.commons.feeds.FeedState
import com.vitorpamplona.amethyst.commons.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.commons.viewmodels.AccountViewModel
import com.vitorpamplona.quartz.nip71Video.VideoEvent
import kotlin.math.abs

/**
 * The reels feed: one video per screen, snapping to the next on a flick, with the engagement rail
 * beside it. Each page is exactly the viewport height so a reel never shares the screen with its
 * neighbour, which is what makes the feed read as pages rather than as a list.
 */
@Composable
fun ShortsFeedLoaded(
    loaded: FeedState.Loaded,
    listState: LazyListState,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    val items by loaded.feed.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        flingBehavior = rememberOneReelPerFling(listState),
    ) {
        itemsIndexed(
            items.list,
            key = { _, item -> item.idHex },
            contentType = { _, item -> item.event?.kind ?: -1 },
        ) { _, item ->
            if (item.event is VideoEvent) {
                Box(modifier = Modifier.fillParentMaxSize()) {
                    ReelCard(
                        baseNote = item,
                        accountViewModel = accountViewModel,
                        nav = nav,
                    )
                }
            }
        }
    }
}

/**
 * A flick moves the feed by exactly one reel. The stock snap behaviour lets the decay carry a fast
 * fling across several pages before it settles, which is what makes a quick swipe skip reels; here
 * the scroll is animated straight to the neighbouring page boundary instead.
 */
@Composable
private fun rememberOneReelPerFling(listState: LazyListState): FlingBehavior =
    remember(listState) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val page =
                    listState.layoutInfo.viewportSize.height
                        .toFloat()
                if (page <= 0f) return 0f

                val offset = listState.firstVisibleItemScrollOffset.toFloat()
                val flicked = abs(initialVelocity) > FLING_THRESHOLD_PX_PER_SECOND
                val distance =
                    when {
                        flicked && initialVelocity > 0f -> page - offset
                        flicked -> -offset
                        offset >= page / 2f -> page - offset
                        else -> -offset
                    }

                if (distance != 0f) {
                    var travelled = 0f
                    animate(
                        initialValue = 0f,
                        targetValue = distance,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    ) { value, _ ->
                        val delta = value - travelled
                        travelled = value
                        scrollBy(delta)
                    }
                }
                return 0f
            }
        }
    }

private const val FLING_THRESHOLD_PX_PER_SECOND = 150f
