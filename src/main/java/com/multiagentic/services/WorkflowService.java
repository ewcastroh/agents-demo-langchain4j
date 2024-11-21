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
}
