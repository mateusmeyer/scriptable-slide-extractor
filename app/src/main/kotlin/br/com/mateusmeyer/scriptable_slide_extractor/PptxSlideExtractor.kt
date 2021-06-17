package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.InputStream

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;
import br.com.mateusmeyer.scriptable_slide_extractor.model.Slide;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideExtractor;

class PptxSlideExtractor : SlideExtractor  {
    constructor(inputStream: InputStream) {

    }

    override fun presentation(): Presentation {
        return null!!
    }

    override fun slides(): List<Slide> {
        return listOf()
    }
}
