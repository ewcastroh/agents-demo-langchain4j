package com.multiagentic;

import java.util.Scanner;

import com.multiagentic.services.WorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentsDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(WorkflowService workflowService) {
        return args -> {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("""
                    Welcome to the Python CLI Application Generator!

                    Please describe the requirements for the application you need.
                    Clearly specify the desired functionality in a concise manner,
                    ensuring it can be implemented in a single Python file.
                    """);

                while (true) {
                    System.out.println("""
                    Enter your requirements below (type 'exit' to close the program):
                    Example: "Create a Python CLI that converts temperatures between Celsius and Fahrenheit."
                    """);
                    System.out.print("> ");
                    String userInput = scanner.nextLine().trim();

                    if (userInput.equalsIgnoreCase("exit")) {
                        System.out.println("Exiting...");
                        break;
                    }

                    if (userInput.isEmpty()) {
                        System.out.println("No input provided. Please enter your requirements or type 'exit' to close the program.");
                        continue;
                    }

                    try {
                        String response = workflowService.generateScript(userInput);
                        System.out.println("\n--- Result ---\n");
                        System.out.println(response);
                        System.out.println("\n----------------\n");
                    } catch (Exception e) {
                        System.err.println("An error occurred while processing your request: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("An unexpected error occurred: " + e.getMessage());
            }
        };
    }
}
