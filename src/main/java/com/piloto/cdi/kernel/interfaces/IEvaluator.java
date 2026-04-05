package com.piloto.cdi.kernel.interfaces;

import com.piloto.cdi.kernel.model.GoalContract;
import com.piloto.cdi.kernel.model.SnapshotState;

public interface IEvaluator {
    EvaluationResult evaluate(Object input, GoalContract goal, SnapshotState state);
}
