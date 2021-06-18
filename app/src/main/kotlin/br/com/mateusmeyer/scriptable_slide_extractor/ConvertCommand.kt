package br.com.mateusmeyer.scriptable_slide_extractor

import kotlin.system.exitProcess
import kotlinx.coroutines.sync.Semaphore

import java.io.File
import java.nio.file.Path

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.flag

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;
import br.com.mateusmeyer.scriptable_slide_extractor.model.SlideConverterPayload;

class ConvertCommand : CliktCommand(name="convert") {

    val scriptPath: Path by argument(help = "Script extraction path (file/folder)")
        .path(mustExist = true, mustBeReadable = true)
    val testFiles: List<Path> by argument(help = "File(s) for testing")
        .path(mustExist = true, mustBeReadable = true)
        .multiple()

    val ignoreNonConvertable by option("-i", "--ignore-non-convertable", help="Ignore files that cannot be converted")
        .flag()

    override fun run() {
        val runner = MainRunner()
        runner.loadScriptFiles(scriptPath)
        runner.loadSlideFiles(testFiles)

        val parentConcurrency = (currentContext.parent?.command as ScriptableSlideExtractor)
            .concurrency

        val semaphore = Semaphore(parentConcurrency)

        runner.doCompileScripts(semaphore)
        runner.doSortScripts()
        runner.doTestFiles(semaphore)
            .let(::abortOnNonConvertibleFiles)
            .let(::convertFiles)
        
    }

    protected fun abortOnNonConvertibleFiles(results: Map<String, Pair<SlideConverter?, Presentation>>): Map<String, Pair<SlideConverter?, Presentation>> {
        val empties = results.filter { (file, pair) ->
            val (converter) = pair;
            converter == null
        }

        if (!ignoreNonConvertable) {
            if (!empties.isEmpty()) {
                println("\nThere's some files that aren't processable in any of provided converters.\n")
                for ((file) in empties) {
                    println(file)
                }

                exitProcess(1)
            }
        }

        return results
    }

    protected fun convertFiles(results: Map<String, Pair<SlideConverter?, Presentation>>) {
        val foundFiles = results.size;

        makeProgressBar(
            "Converting Files",
            foundFiles.toLong() * 2,
        ).use {bar ->
            for ((file, pair) in results) {
                bar.step()

                val (converter, presentation) = pair
                converter?.props?.convert?.invoke(SlideConverterPayload(
                    presentation = presentation
                ))
		
                bar.step()
            }
        }
    }

}

