package com.metumxs.filmlibraryapi;

import org.springframework.boot.SpringApplication;

public class TestFilmLibraryApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FilmLibraryApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
