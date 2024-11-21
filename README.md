# Agents Demo

## Overview

Agents Demo is a Spring Boot application that processes user instructions to generate Python CLI scripts. The application uses a state machine to handle the workflow of processing instructions and generating scripts.

## Features

- Accepts JSON input with user instructions via a REST API.
- Processes instructions using a state machine.
- Generates Python CLI scripts based on user requirements.

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher
- An OpenAI API key

## Getting Started

### Clone the Repository

```sh
git clone https://github.com/ecastro-clgx/agents-demo.git

cd agents-demo
```

### Configuration
Update `pom.xml` file with the correct dependencies and Java version:

```xml
<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>0.36.0</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.statemachine</groupId>
        <artifactId>spring-statemachine-starter</artifactId>
        <version>4.0.0</version>
    </dependency>
</dependencies>
```

To work with the OpenAI Chat Model, add the following dependency:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>0.36.0</version>
</dependency>
```
To work with the Ollama Chat Model, add the following dependency:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama-spring-boot-starter</artifactId>
    <version>0.36.0</version>
</dependency>
```

Update the `src/main/resources/application.properties` file with your OpenAI API key and the `desired model:

To work with the OpenAI Chat Model, you need to set the following properties:
```properties
# OpenAI
langchain4j.open-ai.chat-model.api-key=${OPENAI_API_KEY}
langchain4j.open-ai.chat-model.model-name=gpt-4o-mini
langchain4j.open-ai.chat-model.temperature=0.4
```

To work with the Ollama Model, you need to set the following properties:
```properties
# Ollama
langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3.1
langchain4j.ollama.chat-model.temperature=0.4
```

### Build the Application

```sh
mvn clean install
```

### Run the Application

```sh
mvn spring-boot:run
```

# Steps to create our Application
1. Create Agents
   1. Create a new package named `agents` in the `src/main/java/com/multiagentic` directory.
   2. Create a new interface named `RequirementsEvaluator` in the `agents` package.
        ```java
        @AiService
        public interface RequirementsEvaluator {
        
            @UserMessage("""
                    Evaluate the given requirements to determine if they are clear, concise, and feasible to implement in a single Python file.
                    Return true if the requirements are clear and achievable; otherwise, return false.
        
                    Requirements: {{requirements}}
                    """)
            boolean areRequirementsFeasible(@V("requirements") String requirements);
        }
        ```
   3. Create a new class named `ScriptGenerator` in the `agents` package.
        ```java
        @AiService
        public interface ScriptGenerator {
            @UserMessage("""
                You are an expert Python developer. Create only the Python CLI application script based on the given requirements.
                Do not include any explanations, comments, or additional text—only the code itself.
            
                Requirements: {{requirements}}
                """)
            String generateScript(@V("requirements") String requirements);
        }
        ```
   4. Create a new class named `RequirementsRewriter` in the `agents` package.
        ```java
        @AiService
        public interface RequirementsRewriter {
        
            @UserMessage("""
                The following Python script failed to meet the specified requirements.
                Provide feedback on why it did not meet the requirements and rewrite the requirements by incorporating the necessary improvements while maintaining the original intent.
        
                Requirements: {{requirements}}
                Script: {{script}}
        
                Return the improved requirements.
                """)
            String rewriteRequirements(@V("requirements") String requirements, @V("script") String script);
        }
        ```
   5. Create a new class named `SolutionVerifier` in the `agents` package.
        ```java
        @AiService
        public interface SolutionVerifier {
        
            @UserMessage("""
                Review the provided Python script to ensure it accurately solves the problem as described in the requirements.
                The requirements should be specific, actionable, and focus on a single, clear task.
                
                If the script addresses multiple unrelated tasks, includes ambiguous steps, or lacks sufficient detail for implementation, return false and specify why the requirements are inadequate.
        
                Requirements: {{requirements}}
                Script: {{script}}
                """)
            boolean isScriptValid(@V("script") String script, @V("requirements") String requirements);
        }
        ```
      
2. Create a State Machine Events and States
   1. Create a new package named `workflow` in the `src/main/java/com/multiagentic` directory.
   2. Create a new enum named `Events` in the `workflow` package.
      ```java
      public enum Events {
         INPUT_RECEIVED,
         REQUIREMENTS_EVALUATED,
         REQUIREMENTS_REJECTED,
         SCRIPT_GENERATED,
         SOLUTION_VERIFIED,
         SOLUTION_REJECTED,
         REQUIREMENTS_REWRITTEN
      }
      ```
   3. Create a new enum named `States` in the `workflow` package.
      ```java
      public enum States {
          AWAITING_INPUT,
          REQUIREMENTS_EVALUATION,
          SCRIPT_GENERATION,
          SOLUTION_VERIFICATION,
          REQUIREMENTS_REVISION,
          SUCCESSFUL_COMPLETION,
          INVALID_REQUIREMENTS
      }
      ```
   4. Create `Variables` utility class to store the variables used in the state machine.
      ```java
      public class Variables {
          public static final String REQUIREMENTS = "requirements";
          public static final String SCRIPT = "script";
          public static final String REWRITTEN_REQUIREMENTS = "rewrittenRequirements";
      }
      ```

3. Create a State Machine Configuration
   1. Create a new package named `config` in the `src/main/java/com/multiagentic` directory.
   2. Create a new class named `StateMachineConfig` in the `config` package.
    ```java
    @Configuration
    @EnableStateMachineFactory
    public class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {
    
        private static final Logger log = LoggerFactory.getLogger(StateMachineConfig.class);
    
        private final RequirementsEvaluator requirementsEvaluator;
        private final ScriptGenerator scriptGenerator;
        private final SolutionVerifier solutionVerifier;
        private final RequirementsRewriter requirementsRewriter;
    
        public StateMachineConfig(RequirementsEvaluator requirementsEvaluator,
                           ScriptGenerator scriptGenerator,
                           SolutionVerifier solutionVerifier,
                           RequirementsRewriter requirementsRewriter) {
            this.requirementsEvaluator = requirementsEvaluator;
            this.scriptGenerator = scriptGenerator;
            this.solutionVerifier = solutionVerifier;
            this.requirementsRewriter = requirementsRewriter;
        }
    
        // Configure the states of the state machine
        @Override
        public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
            states
                    .withStates()
                    .initial(States.AWAITING_INPUT)
                    .state(States.REQUIREMENTS_EVALUATION, evaluateRequirementsAction())
                    .state(States.SCRIPT_GENERATION, generateScriptAction())
                    .state(States.SOLUTION_VERIFICATION, verifySolutionAction())
                    .state(States.REQUIREMENTS_REVISION, rewriteRequirementsAction())
                    .end(States.SUCCESSFUL_COMPLETION)
                    .end(States.INVALID_REQUIREMENTS);
        }
    
        // Configure the transitions between states
        @Override
        public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
            transitions
                    .withExternal().source(States.AWAITING_INPUT).target(States.REQUIREMENTS_EVALUATION).event(Events.INPUT_RECEIVED).and()
                    .withExternal().source(States.REQUIREMENTS_EVALUATION).target(States.SCRIPT_GENERATION).event(Events.REQUIREMENTS_EVALUATED).and()
                    .withExternal().source(States.REQUIREMENTS_EVALUATION).target(States.INVALID_REQUIREMENTS).event(Events.REQUIREMENTS_REJECTED).and()
                    .withExternal().source(States.SCRIPT_GENERATION).target(States.SOLUTION_VERIFICATION).event(Events.SCRIPT_GENERATED).and()
                    .withExternal().source(States.SOLUTION_VERIFICATION).target(States.SUCCESSFUL_COMPLETION).event(Events.SOLUTION_VERIFIED).and()
                    .withExternal().source(States.SOLUTION_VERIFICATION).target(States.REQUIREMENTS_REVISION).event(Events.SOLUTION_REJECTED).and()
                    .withExternal().source(States.REQUIREMENTS_REVISION).target(States.SCRIPT_GENERATION).event(Events.REQUIREMENTS_REWRITTEN);
        }
    
        // Action to evaluate requirements
        private Action<States, Events> evaluateRequirementsAction() {
            return stateContext -> {
                log.info("Evaluating requirements...");
                var requirements = getVariable(stateContext, Variables.REQUIREMENTS);
                if (requirementsEvaluator.areRequirementsFeasible(requirements)) {
                    sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_EVALUATED);
                } else {
                    sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_REJECTED);
                }
            };
        }
    
        // Action to generate script
        private Action<States, Events> generateScriptAction() {
            return stateContext -> {
                log.info("Generating script...");
                var requirements = getVariable(stateContext, Variables.REQUIREMENTS);
                var script = scriptGenerator.generateScript(requirements);
                stateContext.getExtendedState().getVariables().put(Variables.SCRIPT, script);
                sendEvent(stateContext.getStateMachine(), Events.SCRIPT_GENERATED);
            };
        }

        // Action to verify solution
        private Action<States, Events> verifySolutionAction() {
            return stateContext -> {
                log.info("Verifying solution...");
                var requirements = getVariable(stateContext, Variables.REQUIREMENTS);
                var script = getVariable(stateContext, Variables.SCRIPT);
                if (solutionVerifier.isScriptValid(requirements, script)) {
                    sendEvent(stateContext.getStateMachine(), Events.SOLUTION_VERIFIED);
                } else {
                    sendEvent(stateContext.getStateMachine(), Events.SOLUTION_REJECTED);
                }
            };
        }
    
        // Action to rewrite requirements
        private Action<States, Events> rewriteRequirementsAction() {
            return stateContext -> {
                log.info("Rewriting requirements...");
                var requirements = getVariable(stateContext, Variables.REQUIREMENTS);
                var script = getVariable(stateContext, Variables.SCRIPT);
                var rewrittenRequirements = requirementsRewriter.rewriteRequirements(requirements, script);
                stateContext.getExtendedState().getVariables().put(Variables.REQUIREMENTS, rewrittenRequirements);
                sendEvent(stateContext.getStateMachine(), Events.REQUIREMENTS_REWRITTEN);
            };
        }
    
        // Helper method to get a variable from the state context
        private String getVariable(StateContext<States, Events> stateContext, String key) {
            return stateContext.getExtendedState().getVariables().get(key).toString();
        }
    
        // Helper method to send an event to the state machine
        private void sendEvent(StateMachine<States, Events> stateMachine, Events event) {
            var message = MessageBuilder.withPayload(event).build();
            stateMachine.sendEvent(Mono.just(message)).subscribe();
        }
    }
    ```

4. Create Workflow Service
   1. Create a new package named `services` in the `src/main/java/com/multiagentic` directory.
   2. Create a new class named `WorkflowService` in the `services` package.
    ```java
    @Service
    public class WorkflowService {
    
        private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);
   
        // Factory to create instances of the state machine
        private final StateMachineFactory<States, Events> factory;
    
        WorkflowService(StateMachineFactory<States, Events> factory) {
            this.factory = factory;
        }
    
        /**
         * Generates a script based on the provided requirements.
         * 
         * @param requirements The input requirements for the workflow.
         * @return The generated script.
         */
        public String generateScript(String requirements) {
            // CompletableFuture to hold the result of the state machine execution
            var resultFuture = new CompletableFuture<String>();
            // Create a new state machine instance
            var stateMachine = factory.getStateMachine();
            // Add a listener to the state machine to handle state changes and errors
            addStateListener(stateMachine, resultFuture);
    
            try {
                // Start the state machine reactively and handle errors
                stateMachine.startReactively()
                        .doOnError(resultFuture::completeExceptionally)
                        .subscribe();
    
                // Set the input requirements in the state machine's extended state
                stateMachine.getExtendedState().getVariables().put(Variables.REQUIREMENTS, requirements);
                // Send an event to the state machine to start the workflow
                stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(Events.INPUT_RECEIVED).build())).subscribe();
    
                // Wait for the result with a timeout of 30 seconds
                return resultFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("State machine execution failed: " + e.getMessage());
                throw new IllegalStateException(e);
            } finally {
                stateMachine.stopReactively().block();
            }
        }
    
        /**
         * Adds a listener to the state machine to handle state changes and errors.
         * 
         * @param stateMachine The state machine instance.
         * @param resultFuture The CompletableFuture to hold the result.
         */
        private void addStateListener(StateMachine<States, Events> stateMachine, CompletableFuture<String> resultFuture) {
            stateMachine.addStateListener(new StateMachineListenerAdapter<>() {
                @Override
                public void stateChanged(State<States, Events> from, State<States, Events> to) {
                    if (to != null) {
                        if (to.getId() == States.SUCCESSFUL_COMPLETION) {
                            Object resultObj = stateMachine.getExtendedState().getVariables().get(Variables.SCRIPT);
                            if (resultObj != null) {
                                resultFuture.complete(resultObj.toString());
                            } else {
                                log.error("Script not found at successful completion");
                                resultFuture.completeExceptionally(new IllegalStateException("Script not found at successful completion state"));
                            }
                        } else if (to.getId() == States.INVALID_REQUIREMENTS) {
                            log.warn("Workflow ended due to invalid requirements.");
                            resultFuture.complete("Invalid requirements: Your input is either unclear or too complex.");
                        }
                    }
                }
    
                @Override
                public void stateMachineError(StateMachine<States, Events> stateMachine, Exception exception) {
                    log.error("State machine encountered an error: " + exception.getMessage());
                    resultFuture.completeExceptionally(exception);
                }
            });
        }
    }
    ```
5. Create a REST Controller
    1. Create a new package named `dto` in the `src/main/java/com/multiagentic` directory.
    2. Create a new class named `InstructionRequest` in the `models` package.
    ```java
    public class InstructionRequest {
        private String instruction;

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }
    }
    ```
6. Create a new package named `controllers` in the `src/main/java/com/multiagentic` directory.
   1. Create a new class named `AgentsController` in the `controllers` package.
       ```java
       @RestController
       @RequestMapping("/api")
       public class InstructionController {
    
           private final WorkflowService workflowService;
    
           public InstructionController(WorkflowService workflowService) {
               this.workflowService = workflowService;
           }
    
           @PostMapping("/process-instruction")
           public ResponseEntity<String> processInstruction(@RequestBody InstructionRequest instructionRequest) {
               String instruction = instructionRequest.getInstruction();
    
               if (instruction == null || instruction.trim().isEmpty()) {
                   return ResponseEntity.badRequest().body("No instruction provided.");
               }
    
               try {
                   String response = workflowService.generateScript(instruction);
                   return ResponseEntity.ok(response);
               } catch (Exception e) {
                   return ResponseEntity.status(500).body("An error occurred while processing your request: " + e.getMessage());
               }
           }
       }
       ```
