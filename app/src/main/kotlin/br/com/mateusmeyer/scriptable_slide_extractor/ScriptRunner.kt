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

val INJECT_CONTENT_TOKEN = "/* @__#InjectContent! */"

class ScriptRunner {
    companion object {
        val scriptEngine = ScriptEngineManager()
        val factory = scriptEngine.getEngineByExtension("kts").factory
    }

    val scriptContent: Pair<StringBuilder, StringBuilder>;
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

            if (scriptProperties.containsKey("import.scripts")) {
                for (item in (scriptProperties.get("import.scripts") as String).split(':')) {
                    val path = File(absoluteScriptPath + item)

                    if (path.exists()) {
                        val scriptContent = this.scriptContent.first
                        val importContent = this.scriptContent.second

                        val injectionTokenIndex = scriptContent.indexOf(INJECT_CONTENT_TOKEN)

                        if (injectionTokenIndex >= 0) {
                            val (importScriptContent, importImportContent) = separateImportsAndContent(path)

                            importContent.append(
                                importImportContent.toString() + "\n"
                            )

                            scriptContent.insert(
                                injectionTokenIndex,
                                importScriptContent.toString() + "\n"
                            )
                        } else {
                            throw Exception("Cannot find injection token in generated script")
                        }
                    } else {
                        throw Exception("Cannot find import script ${path.canonicalPath}")
                    }
                }
            }

            if (scriptProperties.containsKey("import.properties")) {
                for (item in (scriptProperties.get("import.properties") as String).split(':')) {
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

        val scriptContent = this.scriptContent.first
        val importContent = this.scriptContent.second
        val script = importContent
            .append(scriptContent.toString())
            .toString()

        val converter = engine.eval(script, scriptContext)

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

    protected fun generateScriptContent(file: File): Pair<StringBuilder, StringBuilder> {
        var fileBuffer = StringBuilder()
        var importBuffer = StringBuilder()
        var inComment = false
        var injectedContent = false

        importBuffer.append("import ${Presentation::class.java.packageName}.*\n")

        file.inputStream().bufferedReader().useLines {lines ->
            for (line in lines) {
                var trimLine = line.trim()

                if (
                    trimLine.isEmpty() or
                    trimLine.startsWith("//") or
                    inComment
                ) {
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if (
                    trimLine.startsWith("import") or
                    trimLine.startsWith("package")
                ) {
                    importBuffer.append(line + '\n')
                    continue;
                }

                if (trimLine.startsWith("/*")) {
                    inComment = true;
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if (trimLine.endsWith("*/")) {
                    inComment = false;
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if (!injectedContent) {
                    fileBuffer.append("""
                        |val converter = bindings["converter"] as (fn: ${SlideConverter::class.qualifiedName}.() -> Unit) -> ${SlideConverter::class.qualifiedName}
                        |val property = bindings["property"] as (String) -> String?
                        |
                        |${INJECT_CONTENT_TOKEN}
                        |
                        |""".trimMargin())

                    injectedContent = true
                }

                fileBuffer.append(line + '\n')
            }
        }

        return Pair(fileBuffer, importBuffer)
    }

    protected fun separateImportsAndContent(file: File): Pair<StringBuilder, StringBuilder> {
        var fileBuffer = StringBuilder()
        var importBuffer = StringBuilder()
        var inComment = false
        
        file.inputStream().bufferedReader().useLines {lines ->
            for (line in lines) {
                var trimLine = line.trim()

                if (trimLine.startsWith("package")) {
                    throw Exception("package declaration cannot be used inside imported scripts")
                }

                if (
                    trimLine.isEmpty() or
                    trimLine.startsWith("//") or
                    inComment
                ) {
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if (trimLine.startsWith("/*")) {
                    inComment = true;
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if (trimLine.endsWith("*/")) {
                    inComment = false;
                    fileBuffer.append(line + '\n')
                    continue;
                }

                if ( 
                    trimLine.startsWith("import")
                ) {
                    importBuffer.append(line + '\n');
                    continue;
                }

                fileBuffer.append(line + '\n')
            }
        }

        return Pair(fileBuffer, importBuffer)
    }

    protected fun getScriptProperties(metaFile: File): Properties? {

        if (metaFile.exists()) {
            val properties = Properties()
            properties.load(getFileReader(metaFile))

            return properties
        }

        return null
    }

    protected fun getFileReader(file: File): InputStreamReader {
        return InputStreamReader(file.inputStream(), Charset.forName("UTF-8"))
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