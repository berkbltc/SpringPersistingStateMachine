package example.persist;

import example.enums.OrderEvents;
import example.enums.OrderStates;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

import java.util.HashMap;
import java.util.UUID;

public class InMemoryPersist implements StateMachinePersist<OrderStates, OrderEvents, UUID> {

    private HashMap<UUID, StateMachineContext<OrderStates, OrderEvents>> storage
            = new HashMap<>();

    @Override
    public void write(StateMachineContext<OrderStates, OrderEvents> stateMachineContext, UUID uuid) throws Exception {
        storage.put(uuid, stateMachineContext);
    }

    @Override
    public StateMachineContext<OrderStates, OrderEvents> read(UUID uuid) throws Exception {
        return storage.get(uuid);
    }
}