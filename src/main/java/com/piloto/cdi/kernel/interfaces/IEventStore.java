package com.piloto.cdi.kernel.interfaces;

import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.types.TenantID;

import java.util.List;

public interface IEventStore {
    void append(DomainEvent event);
    
    List<DomainEvent> loadByTenant(TenantID tenantId);
}
