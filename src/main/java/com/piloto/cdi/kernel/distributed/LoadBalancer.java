package com.piloto.cdi.kernel.distributed;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {

    private final List<String> nodes;
    private final ShardRouter shardRouter;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    public LoadBalancer(List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list cannot be null or empty");
        }

        this.nodes = List.copyOf(nodes);
        this.shardRouter = new ShardRouter(nodes.size());
    }

    public String selectNode(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }

        int shard = shardRouter.route(tenantId);
        return nodes.get(shard);
    }

    public String selectNodeRoundRobin() {
        int index = roundRobinCounter.getAndIncrement() % nodes.size();
        return nodes.get(index);
    }

    public List<String> getNodes() {
        return nodes;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> shardStats = shardRouter.getStatistics();
        
        return Map.of(
            "total_nodes", nodes.size(),
            "shard_statistics", shardStats,
            "round_robin_counter", roundRobinCounter.get()
        );
    }

    public void resetRoundRobin() {
        roundRobinCounter.set(0);
    }
}
