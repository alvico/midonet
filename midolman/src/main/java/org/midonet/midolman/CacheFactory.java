/**
 * CacheFactory.java - Constructs Cache objects.
 *
 * Copyright (c) 2012 Midokura KK. All rights reserved.
 */

package org.midonet.midolman;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.midonet.cache.Cache;
import org.midonet.cache.CacheException;
import org.midonet.midolman.config.MidolmanConfig;
import org.midonet.cassandra.CassandraCache;


public class CacheFactory {
    private static final Logger log =
        LoggerFactory.getLogger(CacheFactory.class);

    public static final int CACHE_EXPIRATION_SECONDS = 60;

    /**
     * Create a Cache object.
     *
     * @param config configuration for the Cache object
     * @return a Cache object
     * @throws org.midonet.cache.CacheException if an error occurs
     */
    public static Cache create(MidolmanConfig config)
        throws CacheException {
        Cache cache = null;
        String cacheType = config.getMidolmanCacheType();

        boolean isValid = false;

        try {
            if (cacheType.equals("cassandra")) {
                isValid = true;
                String servers = config.getCassandraServers();
                String cluster = config.getCassandraCluster();
                String keyspace = config.getCassandraMidonetKeyspace();

                int replicationFactor = config.getCassandraReplicationFactor();

                cache = new CassandraCache(servers, cluster, keyspace,
                                           "nat", replicationFactor,
                                           CACHE_EXPIRATION_SECONDS);
            }
        } catch (Exception e) {
            throw new CacheException("error while creating cache", e);
        }

        if (!isValid) {
            throw new CacheException("unknown cache type");
        }

        return cache;
    }
}