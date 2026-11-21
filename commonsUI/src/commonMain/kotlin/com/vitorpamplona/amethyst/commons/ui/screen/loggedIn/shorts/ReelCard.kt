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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.commons.icons.symbols.Icon
import com.vitorpamplona.amethyst.commons.icons.symbols.MaterialSymbols
import com.vitorpamplona.amethyst.commons.model.Note
import com.vitorpamplona.amethyst.commons.model.navigation.routes.routeReplyTo
import com.vitorpamplona.amethyst.commons.ui.layouts.LocalDisappearingBarState
import com.vitorpamplona.amethyst.commons.ui.layouts.LocalDisappearingScaffoldPadding
import com.vitorpamplona.amethyst.commons.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.commons.ui.note.BoostWithDialog
import com.vitorpamplona.amethyst.commons.ui.note.LikeReaction
import com.vitorpamplona.amethyst.commons.ui.note.ReplyReaction
import com.vitorpamplona.amethyst.commons.ui.note.ShareReaction
import com.vitorpamplona.amethyst.commons.ui.note.UserPicture
import com.vitorpamplona.amethyst.commons.ui.note.ZapReaction
import com.vitorpamplona.amethyst.commons.ui.note.elements.NoteDropDownMenu
import com.vitorpamplona.amethyst.commons.viewmodels.AccountViewModel
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip71Video.VideoEvent

private val ReelActionSize = 30.dp
private val ReelIconTint = Color.White
private val ReelRailSpacing = 18.dp

/**
 * One full-screen reel: the video centred on black, the author and caption over a bottom scrim, and
 * the actions in a vertical rail on the right, the way a short-video feed is read. The media itself
 * is [VideoCardImage], so content warnings, the player and its sizing behave exactly as they do in
 * the list this replaces — only the chrome around it is new.
 */
@Composable
fun ReelCard(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    val event = (baseNote.event as? VideoEvent) ?: return
    val backgroundColor = remember { mutableStateOf(Color.Transparent) }
    val bottomInset = reelBottomInset()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // A portrait video is taller than the page at this width; clip it so it can't paint over
        // the bars instead of being cropped to the reel.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            VideoCardImage(baseNote, event, backgroundColor, accountViewModel)
        }

        ReelCaption(
            baseNote = baseNote,
            event = event,
            accountViewModel = accountViewModel,
            nav = nav,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(end = 88.dp, bottom = 16.dp + bottomInset, start = 12.dp),
        )

        ReelActionRail(
            baseNote = baseNote,
            accountViewModel = accountViewModel,
            nav = nav,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 24.dp + bottomInset),
        )
    }
}

/**
 * What the bottom of the reel must clear right now: the scaffold lays the page out under its bars,
 * so the caption and the rail step up by however much of the bottom bar is still showing. With
 * immersive scrolling off that is the whole bar; once it slides away only the system nav bar is
 * left to clear.
 */
@Composable
private fun reelBottomInset(): Dp {
    val state = LocalDisappearingBarState.current
    val density = LocalDensity.current
    val covered =
        if (state != null && state.bottomHeightLimit > 0f) {
            with(density) { (state.bottomHeightLimit * (1f - state.bottomCollapsedFraction)).toDp() }
        } else {
            LocalDisappearingScaffoldPadding.current.calculateBottomPadding()
        }
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(covered, navBar)
}

/** Author row plus the reel's title and text, over a gradient so white type stays readable. */
@Composable
private fun ReelCaption(
    baseNote: Note,
    event: VideoEvent,
    accountViewModel: AccountViewModel,
    nav: INav,
    modifier: Modifier = Modifier,
) {
    val title = remember(event) { event.title() }
    val text = remember(event) { (event as? Event)?.content.orEmpty() }
    val body = remember(text, title) { text.takeIf { it.isNotBlank() && it != title } }
    val author = baseNote.author

    Column(
        modifier =
            modifier.background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                ),
            ),
    ) {
        if (author != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserPicture(
                    userHex = author.pubkeyHex,
                    size = 30.dp,
                    pictureModifier = Modifier.clip(CircleShape),
                    accountViewModel = accountViewModel,
                    nav = nav,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = author.toBestDisplayName(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (title != null) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (body != null) {
            Text(
                text = body,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The vertical action rail: avatar, like, comment, boost, zap, share, menu. */
@Composable
private fun ReelActionRail(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: INav,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember(baseNote.idHex) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ReelRailSpacing),
    ) {
        ReplyReaction(
            baseNote = baseNote,
            grayTint = ReelIconTint,
            accountViewModel = accountViewModel,
            showCounter = true,
            iconSizeModifier = Modifier.size(ReelActionSize),
        ) {
            nav.nav { routeReplyTo(baseNote, accountViewModel.account) }
        }

        LikeReaction(
            baseNote = baseNote,
            grayTint = ReelIconTint,
            accountViewModel = accountViewModel,
            nav = nav,
            iconSize = ReelActionSize,
            showCounter = true,
        )

        BoostWithDialog(
            baseNote = baseNote,
            editState = null,
            grayTint = ReelIconTint,
            accountViewModel = accountViewModel,
            nav = nav,
            showCounter = true,
        )

        ZapReaction(
            baseNote = baseNote,
            grayTint = ReelIconTint,
            accountViewModel = accountViewModel,
            iconSize = ReelActionSize,
            iconSizeModifier = Modifier.size(ReelActionSize),
            showCounter = true,
            nav = nav,
        )

        ShareReaction(
            note = baseNote,
            accountViewModel = accountViewModel,
            nav = nav,
            grayTint = ReelIconTint,
            barChartModifier = Modifier.size(ReelActionSize),
        )

        Box {
            Icon(
                symbol = MaterialSymbols.MoreVert,
                contentDescription = null,
                tint = ReelIconTint,
                modifier =
                    Modifier
                        .size(ReelActionSize)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
            )

            if (showMenu) {
                NoteDropDownMenu(
                    note = baseNote,
                    onDismiss = { showMenu = false },
                    accountViewModel = accountViewModel,
                    nav = nav,
                    anchoredToButton = true,
                )
            }
        }
    }
}
