import org.redundent.kotlin.xml.*
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideTextBox

// #region Configuration
val hymnal = Hymnal(
    title = property("hymnal.name") ?: throw Exception("Missing hymnal.name in properties"),
    acronym = property("hymnal.acronym") ?: throw Exception("Missing hymnal.acronym in properties"),
)

// TODO Move below configuration to Property File
val titleSeparators = listOf('\u2013' /* – */, '-')
val textSizes = listOf(
    TextSizes(
        normal = 40.0,
        chorus = 34.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 38.0,
        chorus = 36.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 38.0,
        chorus = 38.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 36.0,
        chorus = 36.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 37.0,
        chorus = 37.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 30.0,
        chorus = 30.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 35.0,
        chorus = 35.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 40.0,
        chorus = 40.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 40.0,
        chorus = 40.0,
        isChorusItalic = true,
        title = 14.0,
        author = 12.0
    ),
    TextSizes(
        normal = 18.0,
        chorus = 18.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 34.0,
        chorus = 34.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 24.0,
        chorus = 24.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 32.0,
        chorus = 32.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 39.0,
        chorus = 39.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
    TextSizes(
        normal = 33.0,
        chorus = 33.0,
        isChorusItalic = true,
        title = 14.0,
        author = 14.0
    ),
)

val titleIgnoreUppercaseSentences = arrayOf(
    "a", "e", "dos",
    "do", "de", "da",
    "ao", "à", "na",
    "sem", "pela", "em",
    "por", "com", "um",
    "no", "que", "é",
    "ó", "às", "para",
    "pelo"
)
// #endregion

data class Hymnal(
    val title: String,
    val acronym: String
)

data class TextSizes(
    val normal: Double,
    val chorus: Double,
    val isChorusItalic: Boolean,
    val title: Double,
    val author: Double
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
    val reference: String?,
    val authors: List<String>,
    val verses: List<Pair<String, Boolean>>,
    val slideInfo: List<ExtractedSlideInfo>
)

class PresentationParseException(message: String) : Exception(message) {}


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
        var matchAuthorSize = false

        textLoop@ for (textBox in textBoxes) {
            for (paragraph in textBox.paragraphs) {
                for (textRun in paragraph.textRuns) {

                    val textRunSize = textRun.fontSize

                    if (textRunSize == textSize.normal.toDouble()) {
                        matchContentSize = true
                    } else if (textRunSize == textSize.title.toDouble() && !matchTitleSize) {
                        matchTitleSize = true
                    } else if (textRunSize == textSize.author.toDouble()) {
                        matchAuthorSize = true
                    }

                    if (
                        (textSize.isChorusItalic && textRun.italic && (textRunSize == textSize.chorus.toDouble())) or
                        (!textSize.isChorusItalic && (textSize.normal != textSize.chorus) && (textRunSize == textSize.chorus.toDouble()))
                    ) {
                        matchChorusSize = true
                    }
                }

                continue@textLoop
            }
        }

        if (matchContentSize && matchTitleSize && matchAuthorSize) {
            if (firstMatchWithoutChorus == null) {
                firstMatchWithoutChorus = textSize
            }

            if (matchChorusSize) {
                foundTextSize = textSize
                break
            }
        }
    }

    if (firstMatchWithoutChorus != null && foundTextSize == null) {
        foundTextSize = firstMatchWithoutChorus
    }

    if (foundTextSize == null) {
        throw PresentationParseException("Slide ${slide.number} does not match any font disposition.")
    }

    textLoop@ for (textBox in textBoxes) {
        if (textBox.text.trim() == "") {
            continue
        }

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
                } else if (textRunSize == foundTextSize.author.toDouble()) {
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

fun preserveDelimiterRegex() = Regex("((?<=, )|(?<= ))")

fun humanizeTitle(input: String): String {
    var parts = input.toLowerCase().split(preserveDelimiterRegex())

    parts = parts.mapIndexed(map@ {i, part ->
        if (i > 0) {
            for (ignoreSentence in titleIgnoreUppercaseSentences) {
                if (part.trim() == ignoreSentence) {
                    return@map part
                }
            }
        }

        return@map part.capitalize()
    })

    return parts.joinToString(separator = "")
}

fun parseSlideTitle(textBox: SlideTextBox): Pair<String, String?> {
    val text = textBox.text
    var textParts: List<String>? = null

    for (separator in titleSeparators) {
        if (text.contains(separator)) {
            textParts = text.split(separator)
            break
        }
    }

    if (textParts == null) {
        return Pair(humanizeTitle(text), null)
    }

    val trimText = textParts[0].trim()
    val referenceTitle = if (Character.isDigit(trimText[0])) trimText else null
    return Pair(humanizeTitle(textParts[1].trim()), referenceTitle)
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
    var reference: String? = null
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
        reference = titleInfo.second

        authors = parseSlideAuthors(authorTextBox)
    }

    for (slideInfo in slidesInfo) {
        verses += parseSlideVerses(slideInfo.contentTextBoxes)
    }
    

    if (title != null && authors != null) {
        return ExtractedPresentationInfo(
            title,
            reference,
            authors,
            verses,
            slidesInfo
        )
    }

    throw PresentationParseException("Invalid Presentation Data")
}

fun parsePresentationVerses(
    verses: List<Pair<String, Boolean>>
): Pair<List<Triple<String, String, Boolean>>, String> {
    var finalVerses: List<Triple<String, String, Boolean>> = listOf();
    var finalOrder = "";
    var foundVerses: List<Pair<String, Boolean>> = listOf();

    var chorusCounter = 1
    var verseCounter = 1

    for (i in 0 until verses.size) {
        val pair = verses[i]
        val (_, isChorus) = pair

        var pairFoundIndex = foundVerses.indexOf(pair)
        if (pairFoundIndex >= 0) {
            var key = if (isChorus) {
                "c" + pairFoundIndex
            } else {
                "v" + pairFoundIndex
            }

            finalOrder += key + " "
        } else {
            var key = if (isChorus) {
                "c" + (chorusCounter)++
            } else {
                "v" + (verseCounter)++
            }

            finalVerses += Triple(pair.first, key, pair.second)
            foundVerses += pair
            finalOrder += key + " "
        }
    }

    return Pair(finalVerses, finalOrder)
}

fun parsePresentation(presentation: Presentation): String {
    var slideInfo = getSlidesInfo(presentation);
    val (verses, verseOrder) = parsePresentationVerses(slideInfo.verses)

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
            "comments" {
                if (slideInfo.reference!!.startsWith("0")) {
                    "comment" {
                        - "${hymnal.acronym} ${slideInfo.reference?.replaceFirst(Regex("^0+"), "")} "
                    }
                }
                "comment" {
                    - "${hymnal.acronym} ${slideInfo.reference}"
                }
            }
            "verseOrder" {
                - verseOrder
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
                    attribute("name", hymnal.title)
                    attribute("entry", slideInfo.reference ?: "")
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

            for ((verse, verseKey, isChorus) in verses) {
                "verse" {
                    attribute("name", verseKey)
                    "lines" {
                        if (isChorus) {
                            element("tag", {
                                attribute("name", "it")
                                var lines = verse.split("\n")
                                var linesIterator = lines.iterator()
                                while (linesIterator.hasNext()) {
                                    val text = linesIterator.next();
                                    text(text.trim())

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
                                text(text.trim())

                                if (linesIterator.hasNext()) {
                                    element("br")
                                }
                            }
                        }
                            
                    }
                }
            }
        }
    }.toString()
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