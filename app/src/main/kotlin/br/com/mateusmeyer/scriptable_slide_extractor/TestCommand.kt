package br.com.mateusmeyer.scriptable_slide_extractor

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.delay

import kotlin.system.exitProcess

import java.io.File
import java.nio.file.Path
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.options.default

class TestCommand : CliktCommand(name="test") {

    val scriptPath: Path by argument(help = "Script extraction path (file/folder)")
        .path(mustExist = true, mustBeReadable = true)
    val testFiles: List<Path> by argument(help = "File(s) for testing")
        .path(mustExist = true, mustBeReadable = true)
        .multiple()

    override fun run() {
        val runner = MainRunner()
        runner.loadScriptFiles(scriptPath)
        runner.loadSlideFiles(testFiles)

        val parentConcurrency = (currentContext.parent?.command as ScriptableSlideExtractor)
            .concurrency or 2;

        val semaphore = Semaphore(parentConcurrency)

        doCompileScripts(runner, semaphore)
        doTestFiles(runner, semaphore)
            .let(::printTestFiles)
        
    }

    protected fun printTestFiles(results: Map<String, SlideParser?>) {
        val foundFiles = results.size;

        println("\nFound ${foundFiles} file(s).\n")

        for ((file, parser) in results) {
            if (parser != null) {
                println("${file}: ${parser.props.name}");
            } else {
                println("${file}: --");
            }
        }
    }

    protected fun doCompileScripts(runner: MainRunner, semaphore: Semaphore) {
        makeProgressBar(
            "Compiling Scripts",
            runner.scriptFiles.size.toLong() * 2,
        ).use {bar ->
            runBlocking {
                runner.eachScript { file ->
                    async(Dispatchers.Default) {
                        semaphore.withPermit {
                            bar.step()
                            runner.loadRunner(file)
                            bar.step()
                        }
                    }
                }
            }
        }
    }

    protected fun doTestFiles(runner: MainRunner, semaphore: Semaphore): Map<String, SlideParser?> {
        var foundTests: Map<String, SlideParser?> = ConcurrentHashMap()

        makeProgressBar(
            "Testing Files",
            runner.slideFiles.size.toLong() * 2,
        ).use {bar ->
            runBlocking {
                runner.eachSlideFile { file ->
                    async(Dispatchers.Default) {
                        semaphore.withPermit {
                            bar.step()

                            val extractor = runner.createSlideExtractor(file)
                            val presentation = extractor.presentation()
                            var foundMatchingScriptRunner: ScriptRunner? = null

                            runner.eachScriptRunner {scriptRunner ->
                                if (scriptRunner.test(presentation)) {
                                    foundMatchingScriptRunner = scriptRunner
                                }
                                foundMatchingScriptRunner != null
                            }

                            foundTests += file.path to foundMatchingScriptRunner?.slideParser

                            bar.step()
                        }
                    }
                }
            }
        }

        return foundTests;
    }
}
