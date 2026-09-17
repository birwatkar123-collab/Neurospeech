package com.neurospeech.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `lowercases and trims`() {
        assertEquals("it is an apple", TextNormalizer.normalize("  IT'S AN APPLE. "))
    }

    @Test
    fun `strips punctuation`() {
        assertEquals("apple", TextNormalizer.normalize("apple!!!"))
        assertEquals("an apple", TextNormalizer.normalize("An apple."))
        assertEquals("apple pie", TextNormalizer.normalize("apple-pie"))
        assertEquals("book", TextNormalizer.normalize("(book)"))
    }

    @Test
    fun `collapses whitespace`() {
        assertEquals("a b c", TextNormalizer.normalize("  a \t b \n  c  "))
    }

    @Test
    fun `expands common contractions before stripping punctuation`() {
        assertEquals("it is apple", TextNormalizer.normalize("it's apple"))
        assertEquals("that is an apple", TextNormalizer.normalize("That's an apple"))
        assertEquals("do not", TextNormalizer.normalize("don't"))
        assertEquals("cannot", TextNormalizer.normalize("can't"))
    }

    @Test
    fun `empty and punctuation-only text normalize to empty`() {
        assertNull(TextNormalizer.tokens("...").firstOrNull())
        assertEquals(emptyList<String>(), TextNormalizer.tokens(""))
        assertEquals(emptyList<String>(), TextNormalizer.tokens("!!!"))
    }
}

class TargetMatcherTest {

    private val apple = setOf("apple")

    @Test
    fun `matches exact target word`() {
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("apple", apple))
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("an apple", apple))
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("It's an apple.", apple))
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("APPLE", apple))
    }

    @Test
    fun `does not treat substring as match`() {
        assertEquals(MatchStatus.NOT_RECOGNIZED, TargetMatcher.match("pineapple", apple))
        assertEquals(MatchStatus.NOT_RECOGNIZED, TargetMatcher.match("apples", apple))
        assertEquals(MatchStatus.NOT_RECOGNIZED, TargetMatcher.match("the crabapple", apple))
    }

    @Test
    fun `accepts alternative answers`() {
        val cup = setOf("cup", "mug")
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("it's a mug", cup))
        assertEquals(MatchStatus.RECOGNIZED, TargetMatcher.match("cup", cup))
        assertEquals(MatchStatus.NOT_RECOGNIZED, TargetMatcher.match("glass", cup))
    }

    @Test
    fun `empty transcript is distinguished from a wrong answer`() {
        assertEquals(MatchStatus.NO_SPEECH, TargetMatcher.match("", apple))
        assertEquals(MatchStatus.NO_SPEECH, TargetMatcher.match("...", apple))
        assertEquals(MatchStatus.NOT_RECOGNIZED, TargetMatcher.match("banana", apple))
    }
}