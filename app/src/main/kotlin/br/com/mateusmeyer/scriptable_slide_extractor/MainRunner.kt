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

    fun doCompileScripts() {
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

    fun doTestFiles(semaphore: Semaphore, concurrency: Int): Map<String, Pair<SlideConverter?, Presentation>> {
        var foundTests: ConcurrentHashMap<String, Pair<SlideConverter?, Presentation>> = ConcurrentHashMap()

        doForEachSlide("Testing Files", semaphore, concurrency) { file, _, presentation ->
            var foundMatchingScriptRunner: ScriptRunner? = null

            eachScriptRunner {scriptRunner ->
                if (scriptRunner.test(presentation)) {
                    foundMatchingScriptRunner = scriptRunner
                }
                foundMatchingScriptRunner != null
            }

            foundTests.put(file.path, Pair(foundMatchingScriptRunner?.slideConverter, presentation))
        }

        return foundTests;
    }

    fun doForEachSlide(
        title: String?,
        semaphore: Semaphore,
        concurrency: Int,
        runner: (File, SlideExtractor, Presentation) -> Unit
    ) {
        var pipelines: List<List<File>> = makePipeline(slideFiles, concurrency)

        if (title != null) {
            makeProgressBar(
                title,
                slideFiles.size.toLong() * 2,
            ).use {bar -> doForEachSlideAction(pipelines, semaphore, {bar.step()}, runner)}
        } else {
            doForEachSlideAction(pipelines, semaphore, null, runner)
        }
    }

    protected fun doForEachSlideAction(
        pipelines: List<List<File>>,
        semaphore: Semaphore,
        step: (() -> Unit)? = {},
        runner: (File, SlideExtractor, Presentation) -> Unit,
    ) {
        runBlocking {
            pipelines.forEach {files ->
                async(Dispatchers.Default) {
                    semaphore.withPermit {
                        files.forEach {file ->
                            step?.invoke()

                            val extractor = createSlideExtractor(file)
                            val presentation = extractor.presentation()

                            runner(file, extractor, presentation)

                            step?.invoke()
                        }
                    }
                }
            }
        }
    }
    
    protected fun makePipeline(slideFiles: List<File>, concurrency: Int): List<List<File>> {
        var pipelines: MutableList<List<File>> = ArrayList()
        val maxSize = slideFiles.size
        val chunkSize = maxSize / concurrency
        var index = 0;

        for (i in 0 until concurrency) {
            pipelines.add(slideFiles.subList(index, index + chunkSize))
            index += chunkSize
        }

        return pipelines
    }
}
