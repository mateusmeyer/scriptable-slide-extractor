package br.com.mateusmeyer.scriptable_slide_extractor

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideConverterPayload

fun converter(fn: SlideConverter.() -> Unit) = SlideConverter().apply(fn)

class SlideConverter{
    class Properties {
        var name: String? = null;
        var author: String? = null;
        var version: String? = null;
        var test: ((Presentation) -> Boolean)? = null;
        var convert: ((SlideConverterPayload) -> Unit)? = null;
        var command: ((String, Map<String, String>, Presentation) -> Unit)? = null;
    }

    var props: Properties = Properties()

    inline fun name(name: () -> String) {
        props.name = name()
    }

    inline fun author(author: () -> String) {
        props.author = author()
    }

    inline fun version(version: () -> String) {
        props.version = version()
    }

    fun test(tester: (presentation: Presentation) -> Boolean) {
        props.test = tester
    }

    fun command(command: (command: String, args: Map<String, String>, presentation: Presentation) -> Unit) {
        props.command = command
    }

    fun convert(converter: (payload: SlideConverterPayload) -> Unit) {
        props.convert = converter
    }
}
