package com.neurospeech.app.core

import androidx.annotation.DrawableRes
import com.neurospeech.app.R

/** One picture-naming item shown during the assessment. */
data class AssessmentItem(
    val id: String,
    val expectedAnswer: String,
    @DrawableRes val imageRes: Int,
    val acceptedAnswers: Set<String>,
)

object AssessmentCatalog {
    val items: List<AssessmentItem> = listOf(
        AssessmentItem("apple", "Apple", R.drawable.ic_apple, setOf("apple")),
        AssessmentItem("banana", "Banana", R.drawable.ic_banana, setOf("banana")),
        AssessmentItem("cup", "Cup", R.drawable.ic_cup, setOf("cup", "mug")),
        AssessmentItem("chair", "Chair", R.drawable.ic_chair, setOf("chair")),
        AssessmentItem("book", "Book", R.drawable.ic_book, setOf("book")),
    )
}