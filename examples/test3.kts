converter {
	name {"Has Notes"}
	author {"Mateus Meyer Jiacomelli"}
	version {"1"}

	test {
		it.slides.filter {slide ->
			slide.notes.size > 0
		}.size > 0
	}
}
