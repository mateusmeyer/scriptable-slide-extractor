package br.com.mateusmeyer.scriptable_slide_extractor.model

import java.time.LocalDateTime

enum class SlideTextAlign {
    LEFT,
    RIGHT,
    CENTER,
    JUSTIFY
}

data class SlideColor (
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int? = null
)

data class SlideSize(
    val width: Int,
    val height: Int
)

data class SlideTextRun(
    val text: String,
    val bold: Boolean,
    val italic: Boolean,
    val underlined: Boolean,
    val fontSize: Double?,
    val fontFamily: String?,
    val color: SlideColor?
)

data class SlideParagraph(
    val textRuns: List<SlideTextRun>,
    val textAlign: SlideTextAlign
)

data class SlideTextBox(
    val text: String,
    val textSize: Double,
    val paragraphs: List<SlideParagraph>
)

data class Slide(
    val number: Int?,
    val title: String?,
    val textBoxes: List<SlideTextBox>,
    val notes: List<SlideTextBox>,
)

data class Presentation(
    val fileName: String,
    val filePath: String,
    val pageSize: SlideSize,
    val lastModified: LocalDateTime?,
    val slides: List<Slide>
)

interface SlideExtractor {
    fun presentation(): Presentation
    fun slides(): List<Slide>
}