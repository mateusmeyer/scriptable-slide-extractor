package br.com.mateusmeyer.scriptable_slide_extractor

import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideExtractor;

class MainRunner {
    var scriptFiles: List<File> = ArrayList()
    var scriptRunners: List<ScriptRunner> = ArrayList()
    var slideFiles: List<File> = ArrayList()

    fun loadScriptFiles(path: Path) {
        parseFileOrFolder(path) {file ->
            if (file.extension == "kts" && !file.isHidden()) {
                scriptFiles += file
            }
        }
    }

    fun loadSlideFiles(paths: List<Path>) {
        paths.forEach {path ->
            parseFileOrFolder(path) {file -> 
                if (!file.isHidden()) {
                    slideFiles += file
                }
            }
        }
    }

    fun loadRunner(file: File): ScriptRunner {
        val scriptRunner = ScriptRunner(file);
        scriptRunners += scriptRunner
        scriptRunner.compile();

        return scriptRunner
    }

    fun createSlideExtractor(file: File): SlideExtractor {
        return SlideExtractorDecider(file)
    }

    fun eachScript(onEach: ((file: File) -> Unit)) {
        scriptFiles.forEach {
            onEach(it)
        }
    }

    fun eachScriptRunner(onEach: ((runner: ScriptRunner) -> Boolean?)) {
        for (item in scriptRunners) {
            if (onEach(item) == true) {
                return
            }
        }
    }

    fun eachSlideFile(onEach: ((slideFile: File) -> Unit)) {
        slideFiles.forEach {
            onEach(it)
        }
    }

    fun parseFileOrFolder(path: Path, each: (file: File) -> Unit) {
        val file = path.toFile()

        if (!file.exists()) {
            throw Exception("File/folder '${path}' does not exists.");
        }

        if (file.isDirectory()) {
            file.listFiles().forEach { subfile ->
                each(subfile)
            }
        } else if (file.isFile()) {
            each(file)
        }
    }

    fun doCompileScripts(semaphore: Semaphore) {
        makeProgressBar(
            "Compiling Scripts",
            scriptFiles.size.toLong() * 2,
        ).use {bar ->
            eachScript { file ->
                bar.step()
                loadRunner(file)
                bar.step()
            }
        }
    }

    fun doSortScripts() {
        scriptRunners = scriptRunners.sortedWith(compareBy {it.filename})
    }

    fun doTestFiles(semaphore: Semaphore): Map<String, Pair<SlideConverter?, Presentation>> {
        var foundTests: Map<String, Pair<SlideConverter?, Presentation>> = ConcurrentHashMap()

        makeProgressBar(
            "Testing Files",
            slideFiles.size.toLong() * 2,
        ).use {bar ->
            runBlocking {
                eachSlideFile { file ->
                    async(Dispatchers.Default) {
                        semaphore.withPermit {
                            bar.step()

                            val extractor = createSlideExtractor(file)
                            val presentation = extractor.presentation()
                            var foundMatchingScriptRunner: ScriptRunner? = null

                            eachScriptRunner {scriptRunner ->
                                if (scriptRunner.test(presentation)) {
                                    foundMatchingScriptRunner = scriptRunner
                                }
                                foundMatchingScriptRunner != null
                            }

                            foundTests += file.path to Pair(foundMatchingScriptRunner?.slideConverter, presentation)
                            bar.step()
                        }
                    }
                }
            }
        }

        return foundTests;
    }
    
}
