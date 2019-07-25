package example.runner;

import example.entity.TransitionContext;
import example.enums.OrderEvents;
import example.enums.OrderStates;
import example.persist.InMemoryPersist;
import example.repository.ExampleRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.util.UUID;


@Slf4j
@Component
class Runner implements ApplicationRunner {

    @Autowired
    ExampleRepository exampleRepository;

    Logger log = LoggerFactory.getLogger(Runner.class);
    private final StateMachineFactory<OrderStates, OrderEvents> factory;

    Runner(StateMachineFactory<OrderStates, OrderEvents> aFactory) {
        this.factory = aFactory;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        Long machineId = 12345L;
        StateMachine<OrderStates, OrderEvents> machine;
        machine = this.factory.getStateMachine(machineId.toString());
        machine.getExtendedState().getVariables().putIfAbsent("machineId", machineId);

        if (exampleRepository.existsById(machine.getId())) {   // StateMachine will continue from current state
            System.out.println("sMachine exists in DB");
            TransitionContext tContext = exampleRepository.findByStateMachineID(machine.getId());
            OrderStates latestState = OrderStates.valueOf(tContext.getCurrentState());

            machine.getStateMachineAccessor()
                    .doWithAllRegions(access -> {
                        access.resetStateMachine(new DefaultStateMachineContext<>
                                (latestState, null, null, null, null, String.valueOf(machineId)));
                    });
        }

        machine.start();

        log.info("<< current State name: {} >>", machine.getState().getId().name());

        OrderEvents tempEvent = OrderEvents.PAY;
//      machine.sendEvent(OrderEvents.FULFILL);
        OrderStates previousState = machine.getState().getId();
        Timestamp time = new Timestamp(System.currentTimeMillis());
        machine.sendEvent(tempEvent);
        OrderStates currentState = machine.getState().getId();
        if (!((currentState.name()).equals(previousState.name()))) {    //If State changed
            TransitionContext transitionContext = new TransitionContext(machine.getId(), currentState, previousState, tempEvent, time);
            exampleRepository.save(transitionContext);
            log.info("MachineId: " + machine.getId() + " saved to DB!");
        }

// Transition context taken from DB
//        log.info("smID: {} previous state:{} current state:{} event occured:{} date:{}",
//                temp.getStateMachineID(),
//                temp.getPreviousState(),
//                temp.getCurrentState(),
//                temp.getEventOccured(),
//                temp.getDate().toString());


// delete finished State Machine from DB
        currentState = machine.getState().getId();
        if ((currentState == OrderStates.FULFILLED) || (currentState == OrderStates.CANCELLED)) {
            exampleRepository.deleteById(machine.getId());
            log.info("machine {} deleted from DB as it is terminated", machine.getId());
        }

// In-memory persist test

        Long machineId2 = 77777L;
        StateMachine<OrderStates, OrderEvents> machine2;
        machine2 = this.factory.getStateMachine(machineId2.toString());
        machine2.getExtendedState().getVariables().putIfAbsent("machineId", machineId2);

        InMemoryPersist stateMachinePersist = new InMemoryPersist();
        StateMachinePersister<OrderStates, OrderEvents, UUID> persister = new DefaultStateMachinePersister<>(stateMachinePersist);
        persister.persist(machine, machine.getUuid());
        persister.restore(machine2, machine.getUuid());

        log.info("+++Machine1 was PAID.Machine2 restored from machine1.Now Machine2: " + machine2.getState().getId().name());
        machine2.sendEvent(OrderEvents.FULFILL);
        log.info("+++Now Machine2: " + machine2.getState().getId().name());

    }
}