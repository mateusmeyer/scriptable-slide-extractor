package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.File;
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext
import javax.script.ScriptContext
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;

class ScriptRunner {
    companion object {
        protected val scriptEngine = ScriptEngineManager()
        protected val factory = scriptEngine.getEngineByExtension("kts").factory
    }

    val scriptContent: String;
    lateinit var slideParser: SlideParser;

	constructor(file: File) {
        scriptContent = generateScriptContent(file)
    }

    fun compile() {
        if (!::slideParser.isInitialized) {
            val engine = factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine

            val scriptContext = SimpleScriptContext()
            var bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
            bindings.put("parser", ::parser)
            scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

            val parser = engine.eval(scriptContent, scriptContext)

            if (parser == null) {
                throw Exception("Script doesn't have a 'parser' declaration.")
            } else if (parser !is SlideParser) {
                throw Exception("Script returns an invalid object. Make sure that 'parser' declaration is the last of file!")
            }

            slideParser = parser
        }
    }

    fun test(presentation: Presentation): Boolean = isCompiled {
        slideParser.props.test?.invoke(presentation) ?: false
    }

    protected fun generateScriptContent(file: File): String {
        return (
            """
            import ${Presentation::class.java.packageName}.*
            val parser = bindings["parser"] as (fn: ${SlideParser::class.qualifiedName}.() -> Unit) -> ${SlideParser::class.qualifiedName}

            ${file.readText()}
            """.trimStart())
    }

    protected fun <R> isCompiled(fn: () -> R): R {
        if (::slideParser.isInitialized) {
            return fn()
        } else {
            throw Exception("Script is not compiled yet.")
        }
    }

    
}
