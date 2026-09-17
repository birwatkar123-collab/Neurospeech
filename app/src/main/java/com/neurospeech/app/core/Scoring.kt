package com.neurospeech.app.core

/**
 * Deterministic, offline response matching. There is deliberately no fuzzy
 * matching or LLM grading: a transcript is recognized only when a
 * whole-word token equals one of the accepted answers after normalization.
 */
object TextNormalizer {

    private val CONTRACTION_EXPANSIONS = mapOf(
        "it's" to "it is",
        "that's" to "that is",
        "what's" to "what is",
        "there's" to "there is",
        "here's" to "here is",
        "i'm" to "i am",
        "you're" to "you are",
        "we're" to "we are",
        "they're" to "they are",
        "can't" to "cannot",
        "couldn't" to "could not",
        "won't" to "will not",
        "wouldn't" to "would not",
        "shouldn't" to "should not",
        "don't" to "do not",
        "doesn't" to "does not",
        "didn't" to "did not",
        "isn't" to "is not",
        "aren't" to "are not",
        "wasn't" to "was not",
        "weren't" to "were not",
        "haven't" to "have not",
        "hasn't" to "has not",
        "i'll" to "i will",
        "you'll" to "you will",
        "let's" to "let us",
        "i'd" to "i would",
        "you'd" to "you would",
        "he's" to "he is",
        "she's" to "she is",
        "it'll" to "it will",
    )

    /** Lowercase -> expand simple contractions -> strip punctuation -> collapse whitespace. */
    fun normalize(raw: String): String {
        val lower = raw.lowercase()
        val expanded = CONTRACTION_EXPANSIONS.entries.fold(lower) { acc, (from, to) ->
            acc.replace(from, to)
        }
        return expanded
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun tokens(raw: String): List<String> =
        normalize(raw).split(' ').filter { it.isNotEmpty() }
}

enum class MatchStatus { RECOGNIZED, NOT_RECOGNIZED, NO_SPEECH }

object TargetMatcher {

    /** Matches [transcript] against [acceptedAnswers] using exact word tokens only. */
    fun match(transcript: String, acceptedAnswers: Set<String>): MatchStatus {
        val tokens = TextNormalizer.tokens(transcript)
        if (tokens.isEmpty()) return MatchStatus.NO_SPEECH
        val accepted = acceptedAnswers.mapTo(mutableSetOf()) { it.lowercase() }
        return if (tokens.any { it in accepted }) MatchStatus.RECOGNIZED
        else MatchStatus.NOT_RECOGNIZED
    }
}