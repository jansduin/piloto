package com.piloto.cdi.kernel.distributed;

import java.util.HashMap;
import java.util.Map;

public class ShardRouter {

    private final int numShards;
    private final Map<String, Integer> cachedRoutes = new HashMap<>();

    public ShardRouter(int numShards) {
        if (numShards <= 0) {
            throw new IllegalArgumentException("Number of shards must be positive");
        }
        this.numShards = numShards;
    }

    public int route(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }

        return cachedRoutes.computeIfAbsent(tenantId, this::computeShard);
    }

    private int computeShard(String tenantId) {
        int hash = tenantId.hashCode();
        int positiveHash = hash & 0x7FFFFFFF;
        return positiveHash % numShards;
    }

    public Map<String, Integer> getShardDistribution() {
        return Map.copyOf(cachedRoutes);
    }

    public int getNumShards() {
        return numShards;
    }

    public void clearCache() {
        cachedRoutes.clear();
    }

    public Map<String, Object> getStatistics() {
        Map<Integer, Integer> shardCounts = new HashMap<>();
        
        for (int shard : cachedRoutes.values()) {
            shardCounts.merge(shard, 1, Integer::sum);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_tenants", cachedRoutes.size());
        stats.put("num_shards", numShards);
        stats.put("shard_counts", Map.copyOf(shardCounts));
        
        if (!cachedRoutes.isEmpty()) {
            double avgTenantsPerShard = (double) cachedRoutes.size() / numShards;
            stats.put("avg_tenants_per_shard", avgTenantsPerShard);
        } else {
            stats.put("avg_tenants_per_shard", 0.0);
        }

        return Map.copyOf(stats);
    }
}
