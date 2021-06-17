package br.com.mateusmeyer.scriptable_slide_extractor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.options.default


class ScriptableSlideExtractor : CliktCommand(help = "Test files over extractors") {
    val concurrency: Int by option(help="Number of concurrent processment")
        .int()
        .default(2)

    override fun run() = Unit
}

fun main(args: Array<String>) = ScriptableSlideExtractor()
    .subcommands(TestCommand())
    .main(args)
