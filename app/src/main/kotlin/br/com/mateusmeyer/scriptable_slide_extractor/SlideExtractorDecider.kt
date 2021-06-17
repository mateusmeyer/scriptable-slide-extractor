package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.File

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;
import br.com.mateusmeyer.scriptable_slide_extractor.model.Slide;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideExtractor;

class SlideExtractorDecider : SlideExtractor {
    val file: File;
    val extractor: SlideExtractor;

    constructor(file: File) {
        val filePath = file.path
        
        if (!file.exists()) {
            throw Exception("File '${filePath}' does not exists.")
        }

        extractor = when (file.extension) {
            "pptx" -> PptxSlideExtractor(file.inputStream())
            "ppt" -> PptSlideExtractor(file)
            else -> throw Exception("File '${filePath} is not supported.")
        }
        
        this.file = file
    }

    override fun presentation(): Presentation {
        return extractor.presentation()
    }

    override fun slides(): List<Slide> {
        return extractor.slides();
    }
}
