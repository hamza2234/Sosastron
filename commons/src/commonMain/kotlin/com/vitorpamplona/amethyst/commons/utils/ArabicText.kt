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
package com.vitorpamplona.amethyst.commons.utils

private fun Char.isArabicLetter(): Boolean =
    this in '\u0600'..'\u06FF' ||
        this in '\u0750'..'\u077F' ||
        this in '\u08A0'..'\u08FF' ||
        this in '\uFB50'..'\uFDFF' ||
        this in '\uFE70'..'\uFEFF'

/** Links and nostr: URIs: Latin noise that would otherwise drown an Arabic post's ratio. */
private val nonProse = Regex("""(https?://\S+)|(nostr:\S+)|(www\.\S+)""")

/**
 * True when a post reads as Arabic: at least [minRatio] of its prose letters are Arabic script.
 * Used by the Arab Community filter, which keeps the already-loaded feed and drops the rest.
 */
fun String.hasArabicContent(minRatio: Float = 0.3f): Boolean {
    val prose = nonProse.replace(this, " ")
    var arabic = 0
    var letters = 0
    for (c in prose) {
        if (c.isLetter()) {
            letters++
            if (c.isArabicLetter()) arabic++
        }
    }
    if (arabic == 0) return false
    return letters == 0 || arabic.toFloat() / letters >= minRatio
}
