package com.piloto.cdi.kernel.upcaster;

import com.piloto.cdi.kernel.model.DomainEvent;

/**
 * Interface that defines the contract for upcasting a Domain Event from one
 * schema version to another.
 * This is used to maintain backward compatibility of events when domain models
 * evolve.
 */
public interface IEventUpcaster {

    /**
     * Checks if this upcaster can process the given event.
     * 
     * @param event The event loaded from the store
     * @return true if the upcaster knows how to transform this event
     */
    boolean canUpcast(DomainEvent event);

    /**
     * Transforms the event into a new version.
     * 
     * @param event the original event
     * @return a new DomainEvent representing the upgraded event
     */
    DomainEvent upcast(DomainEvent event);
}
