package com.pardorps.pardrops;

import com.pardorps.pardrops.config.DatabaseUrlSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PardropsApplication {

	public static void main(String[] args) {
		// Neon entrega la conexión como DATABASE_URL (postgresql://...). Spring
		// Boot necesita el formato jdbc:postgresql://..., así que se adapta antes
		// de arrancar el contexto.
		DatabaseUrlSupport.applyEnvironmentDatabaseUrl();
		SpringApplication.run(PardropsApplication.class, args);
	}

}
