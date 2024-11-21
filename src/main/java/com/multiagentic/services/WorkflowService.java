package com.multiagentic.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.multiagentic.workflow.Events;
import com.multiagentic.workflow.States;
import com.multiagentic.workflow.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
