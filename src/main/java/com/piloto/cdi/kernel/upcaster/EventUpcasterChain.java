package com.piloto.cdi.kernel.upcaster;

import com.piloto.cdi.kernel.model.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes a sequence of upcasters on a Domain Event until it reaches the
 * latest schema version.
 */
public class EventUpcasterChain {
    private final List<IEventUpcaster> upcasters;

    public EventUpcasterChain() {
        this.upcasters = new ArrayList<>();
    }

    public EventUpcasterChain(List<IEventUpcaster> upcasters) {
        this.upcasters = new ArrayList<>(upcasters);
    }

    public void addUpcaster(IEventUpcaster upcaster) {
        this.upcasters.add(upcaster);
    }

    /**
     * Iteratively applies matching upcasters to the event.
     * 
     * @param event The original event
     * @return The fully upcasted event
     */
    public DomainEvent upcast(DomainEvent event) {
        DomainEvent current = event;
        boolean upcasted;
        do {
            upcasted = false;
            for (IEventUpcaster upcaster : upcasters) {
                if (upcaster.canUpcast(current)) {
                    current = upcaster.upcast(current);
                    upcasted = true;
                    // Restart loop to see if another upcaster (or the same one for next version)
                    // applies
                    break;
                }
            }
        } while (upcasted);

        return current;
    }
}
