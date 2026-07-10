package com.dnd.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Класс SuperManagerofHeresyforIdiotsApplication описывает компонент приложения, который участвует в бизнес-логике backend.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@SpringBootApplication
@EnableScheduling
public class SuperManagerofHeresyforIdiotsApplication {

	/**
	 * Выполняет операции "main" в рамках бизнес-логики приложения.
	 * @param args входящее значение args, используемое бизнес-сценарием
	 */
	public static void main(String[] args) {
		SpringApplication.run(SuperManagerofHeresyforIdiotsApplication.class, args);
	}

}
