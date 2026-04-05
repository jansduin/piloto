package com.piloto.cdi.kernel.interfaces;

import com.piloto.cdi.kernel.model.GoalContract;
import com.piloto.cdi.kernel.types.GoalID;
import com.piloto.cdi.kernel.types.TenantID;

import java.util.List;
import java.util.Optional;

public interface IGoalRepository {
    void save(GoalContract goal);
    
    Optional<GoalContract> load(GoalID id);
    
    List<GoalContract> loadByTenant(TenantID tenantId);
}
