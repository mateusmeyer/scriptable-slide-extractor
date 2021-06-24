package br.com.mateusmeyer.scriptable_slide_extractor

import kotlinx.coroutines.sync.Semaphore

import java.io.File
import java.nio.file.Path
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.types.path

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;

class TestCommand : CliktCommand(name="test") {

    val scriptPath: Path by argument(help = "Script(s) path")
        .path(mustExist = true, mustBeReadable = true)
    val testFiles: List<Path> by argument(help = "File(s) for testing")
        .path(mustExist = true, mustBeReadable = true)
        .multiple()

    val showNonConvertableOnly by option("-n", "--non-convertable-only", help="Show only non-convertable files")
        .flag()

    override fun run() {
        val runner = MainRunner()
        runner.loadScriptFiles(scriptPath)
        runner.loadSlideFiles(testFiles)

        val parentConcurrency = (currentContext.parent?.command as ScriptableSlideExtractor)
            .concurrency

        val semaphore = Semaphore(parentConcurrency)

        runner.doCompileScripts()
        runner.doSortScripts()
        runner.doTestFiles(semaphore, parentConcurrency)
            .let(::printTestFiles)
        
    }

    protected fun printTestFiles(results: Map<String, Pair<SlideConverter?, Presentation>>) {
        var empties = 0

        if (showNonConvertableOnly) {
            empties = results.filter { (_, pair) ->
                val (converter) = pair;
                converter == null
            }.size
        }

        println("\nFound ${results.size} file(s); ${empties} aren't processable.")
        println("The following files aren't processable by any of provided converters:\n")

        for ((file, pair) in results) {
            val (converter) = pair

            if (converter != null) {
                if (!showNonConvertableOnly) {
                    println("${file}: ${converter.props.name}");
                }
            } else {
                println("${file}: --");
            }
        }
    }
}
