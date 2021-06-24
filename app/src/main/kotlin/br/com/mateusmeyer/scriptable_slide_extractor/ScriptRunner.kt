package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.File;
import java.io.InputStreamReader
import java.util.Properties
import java.net.URLClassLoader
import java.net.URL
import java.nio.charset.Charset
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
    val scriptProperties: Properties?;
    val absoluteScriptPath: String;
    lateinit var slideConverter: SlideConverter;
    val filename: String;

	constructor(file: File) {
        filename = file.name
        scriptContent = generateScriptContent(file)
        scriptProperties = getScriptProperties(File(file.absolutePath + ".properties"))
        absoluteScriptPath = file.absolutePath.replace(file.name, "");
    }

    fun compile() {
        if (!::slideConverter.isInitialized) {
            var scriptProperties: Properties = this.scriptProperties ?: Properties()
            var threadClassLoader: MutableURLClassLoader? = null;

            if (scriptProperties.containsKey("classpath")) {
                for (item in (scriptProperties.get("classpath") as String).split(':')) {
                    val url = File(absoluteScriptPath + item).toURI().toURL()

                    if (threadClassLoader == null) {
                        threadClassLoader = MutableURLClassLoader(arrayOf(url), this::class.java.classLoader)
                    } else {
                        threadClassLoader.addURL(url)
                    }
                }
            }

            if (scriptProperties.containsKey("import")) {
                for (item in (scriptProperties.get("import") as String).split(':')) {
                    val path = absoluteScriptPath + item
                    val oldProperties = scriptProperties
                    val newProperties = getScriptProperties(File(path))

                    if (newProperties != null) {
                        scriptProperties = Properties()
                        scriptProperties.putAll(oldProperties)
                        scriptProperties.putAll(newProperties)
                    }
                }
            }

            // we need to run a new thread, so we can use
            // a custom classloader
            if (threadClassLoader != null) {
                val thread = Thread({compileRunner(scriptProperties)})
                thread.contextClassLoader = threadClassLoader;
                thread.start();
                thread.join();
            } else {
                compileRunner(scriptProperties)
            }

        }
    }

    fun test(presentation: Presentation): Boolean = isCompiled {
        slideConverter.props.test?.invoke(presentation) ?: false
    }

    fun command(command: String, args: Map<String, String>, presentation: Presentation) {
        slideConverter.props.command?.invoke(command, args, presentation)
    }

    protected fun compileRunner(properties: Properties) {
        val engine = factory.scriptEngine as KotlinJsr223JvmLocalScriptEngine
        val scriptContext = SimpleScriptContext()
        var bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)
        bindings.put("converter", ::converter)
        bindings.put("property", {key: String -> properties.getProperty(key)})
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

        val converter = engine.eval(scriptContent, scriptContext)

        if (converter == null) {
            throw Exception("Script doesn't have a 'converter' declaration.")
        } else if (converter !is SlideConverter) {
            throw Exception("Script returns an invalid object. Make sure that 'converter' declaration is the last of file!")
        }

        slideConverter = converter
    }

    protected fun getCurrentClassPath(): String {
        return System.getProperty("java.class.path")
    }

    protected fun generateScriptContent(file: File): String {
        var fileBuffer: String = "";
        var inComment = false;
        var injectedContent = false;

        file.inputStream().bufferedReader().useLines {lines ->
            for (line in lines) {
                var trimLine = line.trim();

                if (
                    trimLine.isEmpty() or
                    trimLine.startsWith("import") or
                    trimLine.startsWith("package") or
                    trimLine.startsWith("//") or
                    inComment
                ) {
                    fileBuffer += line + '\n'
                    continue;
                }

                if (trimLine.startsWith("/*")) {
                    inComment = true;
                    fileBuffer += line + '\n'
                    continue;
                }

                if (trimLine.endsWith("*/")) {
                    inComment = false;
                    fileBuffer += line + '\n'
                    continue;
                }

                if (!injectedContent) {
                    fileBuffer += """
                        import ${Presentation::class.java.packageName}.*
                        val converter = bindings["converter"] as (fn: ${SlideConverter::class.qualifiedName}.() -> Unit) -> ${SlideConverter::class.qualifiedName}
                        val property = bindings["property"] as (String) -> String?
                        """.trimStart()

                    injectedContent = true
                }

                fileBuffer += line + '\n'
            }
        }

        return fileBuffer
    }

    protected fun getScriptProperties(metaFile: File): Properties? {

        if (metaFile.exists()) {
            val properties = Properties()
            properties.load(InputStreamReader(metaFile.inputStream(), Charset.forName("UTF-8")))

            return properties
        }

        return null
    }

    protected fun <R> isCompiled(fn: () -> R): R {
        if (::slideConverter.isInitialized) {
            return fn()
        } else {
            throw Exception("Script is not compiled yet.")
        }
    }
}

class MutableURLClassLoader : URLClassLoader {
    constructor(url: Array<URL>, parent: ClassLoader): super(url, parent)

    override public fun addURL(url: URL) {
        super.addURL(url);
    }
}