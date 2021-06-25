converter {
	name {"Hinos Padronizados JO"}
	author {"Mateus Meyer Jiacomelli"}
	version {"1"}

    // #region Configuration
    val hymnal = Hymnal(
        title = property("hymnal.name") ?: throw Exception("Missing hymnal.name in properties"),
        acronym = property("hymnal.acronym") ?: throw Exception("Missing hymnal.acronym in properties"),
    )

    val titleSeparators = listOf('\u2013' /* – */, '-', '/')
    val textSizes = listOf(
        TextSizes(
            normal = 40.0,
            chorus = 34.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 38.0,
            chorus = 36.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 38.0,
            chorus = 38.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 36.0,
            chorus = 36.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 37.0,
            chorus = 37.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 30.0,
            chorus = 30.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 35.0,
            chorus = 35.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 40.0,
            chorus = 40.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 40.0,
            chorus = 40.0,
            isChorusItalic = true,
            title = 14.0,
            author = 12.0
        ),
        TextSizes(
            normal = 18.0,
            chorus = 18.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 34.0,
            chorus = 34.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 24.0,
            chorus = 24.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 32.0,
            chorus = 32.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 39.0,
            chorus = 39.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
        TextSizes(
            normal = 33.0,
            chorus = 33.0,
            isChorusItalic = true,
            title = 14.0,
            author = 14.0
        ),
    )

    val titleIgnoreUppercaseSentences = arrayOf(
        "a", "e", "dos",
        "do", "de", "da",
        "ao", "à", "na", "o",
        "sem", "pela", "em",
        "por", "com", "um",
        "no", "que", "é",
        "ó", "às", "para",
        "pelo"
    )
    // #endregion

    val simpleConverter = SimpleSlide2OpenLyricsConverter(
        hymnal, textSizes, titleSeparators, titleIgnoreUppercaseSentences,
        authorMapper = ::authorMapper
    )

	test { presentation ->
        try {
            simpleConverter.getSlidesInfo(presentation)
            true
        } catch (ex: PresentationParseException) {
            false
        }
	}

    convert { data ->

    }

    command { command, args, presentation ->
        when(command) {
            "generate" -> {
                var parsed = simpleConverter.parsePresentation(presentation)
                println(parsed)
            }
            "title" -> {
                try {
                    var info = simpleConverter.getSlidesInfo(presentation)
                    println(info.title)
                } catch (e: PresentationParseException) {
                    println("--")
                }
            }
            "authors" -> {
                try {
                    var info = simpleConverter.getSlidesInfo(presentation)
                    for (author in info.authors) {
                        println(author.name)
                    }
                } catch (e: PresentationParseException) {
                    println("--")
                }
            }
        }
    }

}