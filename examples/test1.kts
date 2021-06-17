
data class TextSizes(
    val normal: Double,
    val chorus: Double,
    val isChorusItalic: Boolean,
    val title: Double
)

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
        isChorusItalic = false,
        title = 14.0
    ),
    TextSizes(
        normal = 37.0,
        chorus = 37.0,
        isChorusItalic = false,
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
        isChorusItalic = false,
        title = 14.0
    ),
)

fun extractInfo(slide: Slide): Pair<List<Pair<SlideTextBox, Boolean>>?, List<SlideTextBox>?> {
    val textBoxes = slide.textBoxes;

    var contentTextBoxes: List<Pair<SlideTextBox, Boolean>> = ArrayList()
    var titleTextBoxes: List<SlideTextBox> = ArrayList()

    textLoop@ for (textBox in textBoxes) {
        for (paragraph in textBox.paragraphs) {
            for (textRun in paragraph.textRuns) {
                var matchTextSize = false

                for (textSize in textSizes) {
                    val textRunSize = textRun.fontSize
                    val isChorusSize = textRunSize == textSize.chorus.toDouble()

                    if (
                        (textRunSize == textSize.normal.toDouble()) or isChorusSize
                    ) {
                        val isTextItalic = textRun.italic
                        val isTextChorus = isChorusSize and (if (textSize.isChorusItalic) isTextItalic else false)

                        contentTextBoxes += Pair(textBox, isTextChorus)
                        matchTextSize = true
                        continue@textLoop
                    } else if (textRunSize == textSize.title.toDouble()) {
                        titleTextBoxes += textBox;
                        matchTextSize = true
                        continue@textLoop
                    }
                }

                if (!matchTextSize) {
                    return Pair(null, null)
                }
            }
        }
    }

    return Pair(contentTextBoxes, titleTextBoxes)
}

converter {
	name {"Hinos Padronizados JO"}
	author {"Mateus Meyer Jiacomelli"}
	version {"1"}

	test { presentation ->
        var allSlidesCorrect = true;

        for (slide in presentation.slides) {
            val extractionInfo = extractInfo(slide)
            allSlidesCorrect = extractionInfo.first != null && extractionInfo.second != null

            if (!allSlidesCorrect) {
                break;
            }
        }

        allSlidesCorrect
	}
}