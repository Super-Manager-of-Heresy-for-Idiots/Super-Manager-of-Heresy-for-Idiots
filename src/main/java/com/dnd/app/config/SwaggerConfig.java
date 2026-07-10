package com.dnd.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Класс SwaggerConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Выполняет операции "open api" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("D&D Character Management API")
                        .description("REST API for managing D&D characters, teams, and reference data")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(contentModelExamples(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer"))));
    }

    /**
     * Reusable reward-group/grant examples for the new content model contract.
     * Endpoints reference these via {@code @ExampleObject(ref = "#/components/examples/<name>")}
     * once they are implemented (Phases 3, 6, 7, 8).
     */
    private Components contentModelExamples(Components components) {
        return components
                .addExamples("autoFeatureGrant", example(
                        "Auto feature grant",
                        "AUTO group that automatically grants a class feature at level 1.",
                        """
                        {
                          "classLevel": 1,
                          "groupKind": "AUTO",
                          "chooseMin": 0,
                          "chooseMax": 0,
                          "repeatable": false,
                          "sortOrder": 0,
                          "options": [],
                          "grants": [
                            { "grantType": "FEATURE", "sortOrder": 0,
                              "payload": { "featureKey": "f_storm_sense" } }
                          ]
                        }"""))
                .addExamples("chooseOneSubclass", example(
                        "Choose one subclass",
                        "CHOICE group where each option grants one subclass.",
                        """
                        {
                          "classLevel": 3,
                          "groupKind": "CHOICE",
                          "prompt": "Choose your storm path",
                          "chooseMin": 1,
                          "chooseMax": 1,
                          "repeatable": false,
                          "sortOrder": 0,
                          "grants": [],
                          "options": [
                            { "optionKey": "path_of_thunder", "label": "Path of Thunder",
                              "recommended": true, "sortOrder": 0,
                              "grants": [
                                { "grantType": "SUBCLASS", "sortOrder": 0,
                                  "payload": { "subclassKey": "sc_thunder" } }
                              ] }
                          ]
                        }"""))
                .addExamples("chooseNSkills", example(
                        "Choose N skills",
                        "Skill proficiency choice from a pool.",
                        """
                        {
                          "grantType": "SKILL_PROFICIENCY",
                          "sortOrder": 0,
                          "payload": {
                            "mode": "CHOICE",
                            "chooseCount": 2,
                            "skillOptionIds": ["sk_arcana", "sk_nature", "sk_perception", "sk_intimidation"]
                          }
                        }"""))
                .addExamples("asiPlus2", example(
                        "ASI +2 to one ability",
                        "Ability score improvement: +2 to a single ability.",
                        """
                        {
                          "grantType": "ABILITY_SCORE",
                          "sortOrder": 0,
                          "payload": { "chooseCount": 1, "bonusPerChoice": 2,
                                       "maxPerAbility": 2, "totalBonus": 2, "maxScore": 20 }
                        }"""))
                .addExamples("asiPlus1Plus1", example(
                        "ASI +1/+1 to two abilities",
                        "Ability score improvement: +1 to two different abilities.",
                        """
                        {
                          "grantType": "ABILITY_SCORE",
                          "sortOrder": 0,
                          "payload": { "chooseCount": 2, "bonusPerChoice": 1,
                                       "maxPerAbility": 1, "totalBonus": 2, "maxScore": 20 }
                        }"""))
                .addExamples("customHomebrewGrant", example(
                        "Custom/manual homebrew grant",
                        "Free-text grant; also the fallback for unknown grantType.",
                        """
                        {
                          "grantType": "CUSTOM_TEXT",
                          "label": "Stormborn",
                          "sortOrder": 0,
                          "payload": {
                            "title": "Stormborn",
                            "body": "You can speak with creatures of the air.",
                            "markdown": false,
                            "userEditable": true
                          }
                        }"""));
    }

    private Example example(String summary, String description, String value) {
        return new Example().summary(summary).description(description).value(value);
    }
}
