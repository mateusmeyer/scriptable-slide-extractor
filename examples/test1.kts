import org.redundent.kotlin.xml.*
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideTextBox

data class TextSizes(
    val normal: Double,
    val chorus: Double,
    val isChorusItalic: Boolean,
    val title: Double
)

data class WithChorusSlideTextBox(
    val textBox: SlideTextBox,
    val chorus: Boolean
)

data class ExtractedSlideInfo(
    val slide: Slide,
    val titleTextBoxes: List<SlideTextBox>,
    val contentTextBoxes: List<WithChorusSlideTextBox>
)

data class ExtractedPresentationInfo(
    val title: String,
    val number: Int?,
    val authors: List<String>,
    val verses: List<Pair<String, Boolean>>,
    val slideInfo: List<ExtractedSlideInfo>
)

class PresentationParseException(message: String) : Exception(message) {}

val titleSeparators = listOf('\u2013' /* – */, '-')
val textSizes = listOf(
    TextSizes(
        normal = 40.0,
        chorus = 34.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 38.0,
        chorus = 38.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 36.0,
        chorus = 36.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 37.0,
        chorus = 37.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 30.0,
        chorus = 30.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 35.0,
        chorus = 35.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 40.0,
        chorus = 40.0,
        isChorusItalic = true,
        title = 14.0
    ),
    TextSizes(
        normal = 18.0,
        chorus = 18.0,
        isChorusItalic = true,
        title = 14.0
    ),
)

fun extractInfo(slide: Slide): ExtractedSlideInfo? {
    val textBoxes = slide.textBoxes

    var contentTextBoxes: List<WithChorusSlideTextBox> = listOf()
    var titleTextBoxes: List<SlideTextBox> = listOf()

    var foundTextSize: TextSizes? = null;
    var firstMatchWithoutChorus: TextSizes? = null;

    for (textSize in textSizes) {
        var matchContentSize = false
        var matchChorusSize = false
        var matchTitleSize = false

        textLoop@ for (textBox in textBoxes) {
            for (paragraph in textBox.paragraphs) {
                for (textRun in paragraph.textRuns) {

                    val textRunSize = textRun.fontSize

                    if (textRunSize == textSize.normal.toDouble()) {
                        matchContentSize = true
                    } else if (textRunSize == textSize.title.toDouble()) {
                        matchTitleSize = true
                    }

                    if (
                        (textSize.isChorusItalic && textRun.italic && (textRunSize == textSize.chorus.toDouble())) or
                        (!textSize.isChorusItalic && (textRunSize == textSize.chorus.toDouble()))
                    ) {
                        matchChorusSize = true
                    }
                }

                continue@textLoop
            }
        }

        if (matchContentSize && matchTitleSize) {
            firstMatchWithoutChorus = textSize
        }

        if (matchContentSize && matchTitleSize && matchChorusSize) {
            foundTextSize = textSize
            break
        }
    }

    if (firstMatchWithoutChorus != null) {
        foundTextSize = firstMatchWithoutChorus
    }

    if (foundTextSize == null) {
        throw PresentationParseException("Slide ${slide.number} does not match any font disposition.")
    }

    textLoop@ for (textBox in textBoxes) {
        for (paragraph in textBox.paragraphs) {
            for (textRun in paragraph.textRuns) {
                val textRunSize = textRun.fontSize
                val isChorusSize = textRunSize == foundTextSize.chorus.toDouble()

                if (
                    (textRunSize == foundTextSize.normal.toDouble()) or isChorusSize
                ) {
                    val isTextItalic = textRun.italic
                    val isTextChorus = isChorusSize and (if (foundTextSize.isChorusItalic) isTextItalic else false)

                    contentTextBoxes += WithChorusSlideTextBox(textBox, isTextChorus)
                    continue@textLoop
                } else if (textRunSize == foundTextSize.title.toDouble()) {
                    titleTextBoxes += textBox
                    continue@textLoop
                } else {
                    throw PresentationParseException("Slide ${slide.number} does not match any font disposition (2).")
                }
            }
        }
    }

    return ExtractedSlideInfo(
        slide,
        titleTextBoxes,
        contentTextBoxes
    )
}

fun parseSlideTitle(textBox: SlideTextBox): Pair<String, Int?> {
    val text = textBox.text
    var textParts: List<String>? = null

    for (separator in titleSeparators) {
        if (text.contains(separator)) {
            textParts = text.split(separator)
            break
        }
    }

    if (textParts == null) {
        return Pair(text, null)
    }

    val int = textParts[0].trim().toInt()
    return Pair(textParts[1].trim(), if (int > 0) int else null)
}

fun parseSlideAuthors(textBox: SlideTextBox): List<String>? {
    val text = textBox.text
    var textParts: List<String>? = null

    for (separator in titleSeparators) {
        if (text.contains(separator)) {
            textParts = text.split(separator)
            break
        }
    }

    if (textParts == null) {
        return listOf(text)
    }

    return textParts?.map {it.trim()}
}

fun parseSlideVerses(textBoxes: List<WithChorusSlideTextBox>): List<Pair<String, Boolean>> {
    return textBoxes.map {it -> Pair(it.textBox.text, it.chorus)}
}

fun getSlidesInfo(presentation: Presentation): ExtractedPresentationInfo {
    var slidesInfo: List<ExtractedSlideInfo> = listOf();
    var title: String? = null
    var number: Int? = null
    var authors: List<String>? = null
    var verses: List<Pair<String, Boolean>> = listOf();

    for (slide in presentation.slides) {
        val extractionInfo = extractInfo(slide)

        if (extractionInfo == null) {
            throw PresentationParseException("Invalid Slide Data")
        }

        slidesInfo += extractionInfo;
    }

    if (slidesInfo[0].titleTextBoxes.size >= 2) {
        val titleTextBoxes = slidesInfo[0].titleTextBoxes
        val titleTextBox = titleTextBoxes[0]
        val authorTextBox = titleTextBoxes[1]

        var titleInfo = parseSlideTitle(titleTextBox)
        title = titleInfo.first
        number = titleInfo.second

        authors = parseSlideAuthors(authorTextBox)
    }

    for (slideInfo in slidesInfo) {
        verses += parseSlideVerses(slideInfo.contentTextBoxes)
    }
    

    if (title != null && authors != null) {
        return ExtractedPresentationInfo(
            title,
            number,
            authors,
            verses,
            slidesInfo
        )
    }

    throw PresentationParseException("Invalid Presentation Data")
}

fun parsePresentation(presentation: Presentation): String {
    var slideInfo = getSlidesInfo(presentation);

    return xml("song") {
        xmlns = "http://openlyrics.info/namespace/2009/song"
        globalProcessingInstruction("xml", "version" to "1.0", "encoding" to "UTF-8")
        attribute("version", "0.8")
        attribute("createdIn", "scriptable-slide-extractor:devel")
        attribute("modifiedIn", "scriptable-slide-extractor:devel")
        attribute("modifiedDate", "TODO ISO8601 Date")

        "properties" {
            "titles" {
                "title" {
                    - slideInfo.title
                }
            }
            "verseOrder" {
                var verseOrderVerseCounter = 1
                var verseOrderChorusCounter = 1

                - slideInfo.verses.fold("") {prev, curr -> prev + if (curr.second) {
                    "c" + (verseOrderChorusCounter++) + " "
                } else {
                    "v" + (verseOrderVerseCounter++) + " "
                }}
            }
            "authors" {
                for (author in slideInfo.authors) {
                    "author" {
                        attribute("type", "music")
                        - author
                    }
                }
            }
            "songbooks" {
                "songbook" {
                    attribute("name", "Hinário")
                    attribute("entry", slideInfo.number ?: "")
                }
            }
        }
        "format" {
            "tags" {
                attribute("application", "OpenLP")
                "tag" {
                    attribute("name", "it")
                    "open" {
                        - "<em>"
                    }
                    "close" {
                        - "</em>"
                    }
                }
            }
        }
        "lyrics" {
            var verseCounter = 1;
            var chorusCounter = 1;

            for ((verse, isChorus) in slideInfo.verses) {
                "verse" {
                    attribute("name", if (isChorus) {
                            "c" + (chorusCounter++)
                        } else {
                            "v" + (verseCounter++)
                        }
                    )
                    "lines" {
                        if (isChorus) {
                            element("tag", {
                                attribute("name", "it")
                                var lines = verse.split("\n")
                                var linesIterator = lines.iterator()
                                while (linesIterator.hasNext()) {
                                    val text = linesIterator.next();
                                    text(text)

                                    if (linesIterator.hasNext()) {
                                        element("br")
                                    }
                                }
                            })
                        } else {
                            var lines = verse.split("\n")
                            var linesIterator = lines.iterator()
                            while (linesIterator.hasNext()) {
                                val text = linesIterator.next();
                                text(text)

                                if (linesIterator.hasNext()) {
                                    element("br")
                                }
                            }
                        }
                            
                    }
                }
            }
        }
    }.toString(false)
}

converter {
	name {"Hinos Padronizados JO"}
	author {"Mateus Meyer Jiacomelli"}
	version {"1"}

	test { presentation ->
        try {
            getSlidesInfo(presentation)
            true
        } catch (ex: PresentationParseException) {
            false
        }
	}

    convert { data ->

    }

    command { command, args, presentation ->
        if (command == "generate") {
            var parsed = parsePresentation(presentation)
            println(parsed)
        }
    }

}