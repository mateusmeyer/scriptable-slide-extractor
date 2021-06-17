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

class PptSlideConverter : SlideConverterBase {
    val slideshow: HSLFSlideShow;

    constructor(inputStream: InputStream) {
        slideshow = HSLFSlideShow(inputStream);
    }

    override fun slides(): List<Slide> {
        return slideshow.slides.map {slide ->

            Slide(
                slide?.slideNumber,
                slide?.title ?: slide?.slideName,
                slideTextBoxes(slide)
            )
        }
    }

    protected fun slideTextBoxes(slide: HSLFSlide?): List<SlideTextBox> {
        return slide?.shapes
            ?.filter({shape -> shape.shapeType == ShapeType.TEXT_BOX})
            ?.map {shape -> 
                SlideTextBox(
                    (shape as HSLFTextShape).text,
                    shape.getTextHeight(null),
                    slideTextBoxParagraphs(shape)
                )
            }
            ?: listOf()
    }

    protected fun slideTextBoxParagraphs(shape: HSLFTextShape): List<SlideParagraph> {
        return shape
            .textParagraphs
            .map {paragraph ->
                SlideParagraph(
                    paragraph.map {textRun -> 
                        SlideTextRun(
                            textRun.rawText,
                            textRun.isBold,
                            textRun.isItalic,
                            textRun.isUnderlined,
                            textRun.fontSize,
                            textRun.fontFamily,
                            SlideColor(
                                textRun.fontColor.solidColor.color.red,
                                textRun.fontColor.solidColor.color.green,
                                textRun.fontColor.solidColor.color.blue,
                                textRun.fontColor.solidColor.color.alpha
                            )
                        )
                    },
                    when (paragraph.textAlign) {
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
