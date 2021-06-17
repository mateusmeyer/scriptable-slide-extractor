package br.com.mateusmeyer.scriptable_slide_extractor

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers

import java.io.File;
import kotlin.system.exitProcess

import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext
import javax.script.ScriptContext
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine

class SlideConverter : SlideConverterBase {
    val file: File;
    val converter: SlideConverterBase;

    constructor(filePath: String) {
        file = File(filePath)

        if (!file.exists()) {
            throw Exception("File '${filePath}' does not exists.")
        }

        converter = when (file.extension) {
            "pptx" -> PptxSlideConverter(file.inputStream())
            "ppt" -> PptSlideConverter(file.inputStream())
            else -> throw Exception("File '${filePath} is not supported.")
        }
    }

    override fun slides(): List<Slide> {
        return converter.slides();
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("No file provided!")
    } else {
        val dslScript = args.first();
        val scriptEngine = ScriptEngineManager()
        val factory = scriptEngine.getEngineByExtension("kts").factory
        val engine = factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine

        val scriptContext = SimpleScriptContext()
        var bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
        bindings.put("extractor", {fn: SlideExtractor.() -> Unit -> extractor(fn)})
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

        var fileContent = "val extractor = bindings[\"extractor\"] as (fn: ${SlideExtractor::class.qualifiedName}.() -> Unit) -> ${SlideExtractor::class.qualifiedName}\n" + File(dslScript).readText()

        val extractor: SlideExtractor? = engine.eval(fileContent, scriptContext) as SlideExtractor?;

        if (extractor == null) {
            System.err.println("Error when compiling extractor!");
            exitProcess(1)
        }

        val converters = args.copyOfRange(1, args.size).map {arg ->
            try {SlideConverter(arg)} catch (e: Exception) {
                System.err.println(e.message)
                null
            }
        }

        if (converters.contains(null)) {
            exitProcess(2);
        }

        runBlocking {
            converters.forEach { converter ->
                async(Dispatchers.Default) {
                    val presentation = Presentation(
                        converter!!.slides()
                    )

                    println(extractor._props.test?.invoke(presentation))
                }
            }
        }
    }
}
