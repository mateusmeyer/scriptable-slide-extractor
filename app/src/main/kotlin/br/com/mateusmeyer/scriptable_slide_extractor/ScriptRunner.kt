package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.File;
import javax.script.ScriptEngineManager
import javax.script.SimpleScriptContext
import javax.script.ScriptContext
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;

class ScriptRunner {
    companion object {
        val scriptEngine = ScriptEngineManager()
        val factory = scriptEngine.getEngineByExtension("kts").factory
    }

    val scriptContent: String;
    lateinit var slideConverter: SlideConverter;
    val filename: String;

	constructor(file: File) {
        filename = file.name
        scriptContent = generateScriptContent(file)
    }

    fun compile() {
        if (!::slideConverter.isInitialized) {
            val engine = factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine

            val scriptContext = SimpleScriptContext()
            var bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
            bindings.put("converter", ::converter)
            scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

            val converter = engine.eval(scriptContent, scriptContext)

            if (converter == null) {
                throw Exception("Script doesn't have a 'converter' declaration.")
            } else if (converter !is SlideConverter) {
                throw Exception("Script returns an invalid object. Make sure that 'converter' declaration is the last of file!")
            }

            slideConverter = converter
        }
    }

    fun test(presentation: Presentation): Boolean = isCompiled {
        slideConverter.props.test?.invoke(presentation) ?: false
    }

    fun command(command: String, args: Map<String, String>, presentation: Presentation) {
        slideConverter.props.command?.invoke(command, args, presentation)
    }

    protected fun generateScriptContent(file: File): String {
        return (
            """
            import ${Presentation::class.java.packageName}.*
            val converter = bindings["converter"] as (fn: ${SlideConverter::class.qualifiedName}.() -> Unit) -> ${SlideConverter::class.qualifiedName}

            ${file.readText()}
            """.trimStart())
    }

    protected fun <R> isCompiled(fn: () -> R): R {
        if (::slideConverter.isInitialized) {
            return fn()
        } else {
            throw Exception("Script is not compiled yet.")
        }
    }

    
}
