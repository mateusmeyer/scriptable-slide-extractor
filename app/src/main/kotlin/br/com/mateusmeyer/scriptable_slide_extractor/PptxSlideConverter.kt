package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.InputStream

class PptxSlideConverter : SlideConverterBase  {
    constructor(inputStream: InputStream) {

    }

    override fun slides(): List<Slide> {
        return listOf()
    }
}
