/*package br.com.mateusmeyer.scriptable_slide_extractor

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
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.types.path

import br.com.mateusmeyer.scriptable_slide_extractor.model.Presentation;

class CommandCommand : CliktCommand(name="command") {

    val scriptPath: Path by argument(help = "Script path")
        .path(mustExist = true, mustBeReadable = true, canBeDir = false)
    val scriptCommand by option(help = "Script Command")
    val scriptCommandArgs: List<String> by option(help = "Script command arguments (key:value)")
        .multiple()
    val testFiles: List<Path> by argument(help = "File(s) for running commands")
        .path(mustExist = true, mustBeReadable = true)
        .multiple()

    override fun run() {
        val runner = MainRunner()
        runner.loadScriptFiles(scriptPath)
        runner.loadSlideFiles(testFiles)

        val args = scriptCommandArgs./

        runner.doCompileScripts()
        doRunCommand(runner, scriptCommand, scriptCommandArgs)
    }

    fun doRunCommand(runner: MainRunner, command: String, args: Map<String, String>) {
        val semaphore = Semaphore(1)
        runner.doForEachSlide("Running Command", semaphore, 1) {file, extractor, presentation -> 
            eachScriptRunner {scriptRunner ->
                scriptRunner.command(command, args, presentation)
            }
        }
    }

}

*/