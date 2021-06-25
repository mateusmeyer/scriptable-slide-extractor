package br.com.mateusmeyer.scriptable_slide_extractor

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import java.time.Duration
import java.time.temporal.ChronoUnit

fun makeProgressBar(
	title: String,
	max: Long
): ProgressBar = ProgressBar(
    title,
    max,
    125,
    System.err,
    ProgressBarStyle.ASCII,
    "",
    2,
    false,
    null,
    ChronoUnit.SECONDS,
    0,
    Duration.ZERO
);
