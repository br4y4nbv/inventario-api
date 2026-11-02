package com.br4y4nbv.inventario_api;

import org.springframework.boot.SpringApplication;

public class TestInventarioApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(InventarioApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
