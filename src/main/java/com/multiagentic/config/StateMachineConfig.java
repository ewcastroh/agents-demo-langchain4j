package com.multiagentic.config;

import com.multiagentic.agents.RequirementsEvaluator;
import com.multiagentic.agents.RequirementsRewriter;
import com.multiagentic.agents.ScriptGenerator;
import com.multiagentic.agents.SolutionVerifier;
import com.multiagentic.workflow.Events;
import com.multiagentic.workflow.States;
import com.multiagentic.workflow.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import reactor.core.publisher.Mono;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {
}
