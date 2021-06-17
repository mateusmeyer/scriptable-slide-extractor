package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.InputStream
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlide
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl
import org.apache.poi.hslf.usermodel.HSLFTextShape
import org.apache.poi.ooxml.POIXMLDocument
import org.apache.poi.sl.usermodel.ShapeType
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;
import br.com.mateusmeyer.scriptable_slide_extractor.model.Slide;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideExtractor;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideColor;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideTextRun;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideParagraph;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideTextBox;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideTextAlign;

class PptSlideExtractor : SlideExtractor {
    val slideshow: HSLFSlideShow;

    constructor(inputStream: InputStream) {
        slideshow = HSLFSlideShow(inputStream);
    }

    override fun presentation(): Presentation {
        return Presentation(
            slides = slides()
        )
    }

    override fun slides(): List<Slide> {
        return slideshow.slides.map {slide ->

            Slide(
                number = slide?.slideNumber,
                title = slide?.title ?: slide?.slideName,
                textBoxes = slideTextBoxes(slide)
            )
        }
    }

    protected fun slideTextBoxes(slide: HSLFSlide?): List<SlideTextBox> {
        return slide?.shapes
            ?.filter({shape -> shape.shapeType == ShapeType.TEXT_BOX})
            ?.map {shape -> 
                SlideTextBox(
                    text = (shape as HSLFTextShape).text,
                    textSize = shape.getTextHeight(null),
                    paragraphs = slideTextBoxParagraphs(shape)
                )
            }
            ?: listOf()
    }

    protected fun slideTextBoxParagraphs(shape: HSLFTextShape): List<SlideParagraph> {
        return shape
            .textParagraphs
            .map {paragraph ->
                SlideParagraph(
                    textRuns = paragraph.map {textRun -> 
                        SlideTextRun(
                            text = textRun.rawText,
                            bold = textRun.isBold,
                            italic = textRun.isItalic,
                            underlined = textRun.isUnderlined,
                            fontSize = textRun.fontSize,
                            fontFamily = textRun.fontFamily,
                            color = SlideColor(
                                textRun.fontColor.solidColor.color.red,
                                textRun.fontColor.solidColor.color.green,
                                textRun.fontColor.solidColor.color.blue,
                                textRun.fontColor.solidColor.color.alpha
                            )
                        )
                    },
                    textAlign = when (paragraph.textAlign) {
                        TextAlign.LEFT -> SlideTextAlign.LEFT
                        TextAlign.CENTER -> SlideTextAlign.CENTER
                        TextAlign.RIGHT -> SlideTextAlign.RIGHT
                        TextAlign.JUSTIFY -> SlideTextAlign.JUSTIFY
                        TextAlign.JUSTIFY_LOW -> SlideTextAlign.JUSTIFY
                        TextAlign.DIST -> SlideTextAlign.JUSTIFY
                        TextAlign.THAI_DIST -> SlideTextAlign.JUSTIFY
                        else -> SlideTextAlign.LEFT
                    },  
                )
            }
    }

}
