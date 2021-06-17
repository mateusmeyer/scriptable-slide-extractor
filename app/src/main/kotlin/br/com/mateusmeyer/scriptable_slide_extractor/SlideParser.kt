package br.com.mateusmeyer.scriptable_slide_extractor

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;

fun parser(fn: SlideParser.() -> Unit) = SlideParser().apply(fn)

class SlideParser {
    class Properties {
        var name: String? = null;
        var author: String? = null;
        var version: String? = null;
        var test: ((Presentation) -> Boolean)? = null;
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
        props.test = tester;
    }
}
