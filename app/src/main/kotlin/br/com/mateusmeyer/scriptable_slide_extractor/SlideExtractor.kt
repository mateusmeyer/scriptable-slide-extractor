package br.com.mateusmeyer.scriptable_slide_extractor

import org.bouncycastle.util.Properties

fun extractor(fn: SlideExtractor.() -> Unit) = SlideExtractor().apply(fn)

class SlideExtractor {
    class Properties {
        var name: String? = null;
        var author: String? = null;
        var version: String? = null;
        var test: ((Presentation) -> Boolean)? = null;
    }

    var _props: Properties = Properties()

    inline fun name(name: () -> String) {
        _props.name = name()
    }

    inline fun author(author: () -> String) {
        _props.author = author()
    }

    inline fun version(version: () -> String) {
        _props.version = version()
    }

    fun test(tester: (presentation: Presentation) -> Boolean) {
        _props.test = tester;
    }
}
