/*
 * Copyright (c) 2019 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.baidu.cloud.starlight.springcloud.client.ribbon;

import com.baidu.cloud.starlight.api.rpc.threadpool.NamedThreadFactory;
import com.baidu.cloud.starlight.springcloud.client.properties.StarlightClientProperties;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.netflix.loadbalancer.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by liuruisen on 2021/7/27.
 */
public class RibbonServerLocalStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RibbonServerLocalStore.class);

    private static final Integer STORE_LOCAL_CACHE_DELAY = 3 * 60 * 1000;

    private final String clientName;

    private final StarlightClientProperties starlightProperties;

    /**
     * Local mem cache of Ribbon Server, stored in local disk
     */
    private Properties localSrvListCache;

    private File cacheFile;

    private static final Gson GSON = new Gson();

    private ScheduledExecutorService storeCacheExecutor;

    public RibbonServerLocalStore(String clientName, StarlightClientProperties clientProperties) {
        this.clientName = clientName;
        this.starlightProperties = clientProperties;
        initLocalCache();
    }

    /**
     * init local cache <1> create disk file </1> <2> load from disk file and store in mem </2>
     */
    public void initLocalCache() {
        // enable local cache
        if (starlightProperties.getLocalCacheEnabled(clientName)) {
            localSrvListCache = new Properties();
            String cacheFilePath = getCacheFileName();
            // 非强依赖，创建不成功仍可以继续执行，只是会缺少本地文件缓存能力
            createCacheFile(cacheFilePath);
            // load server list from disk, will be temporarily stored in memory(Properties)
            loadCachedListOfServers();

            // store cache executor
            storeCacheExecutor =
                Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("StoreLocal-" + clientName));
            storeCacheExecutor.scheduleWithFixedDelay(this::storeCachedListOfServer, STORE_LOCAL_CACHE_DELAY,
                STORE_LOCAL_CACHE_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Load server list from local disk
     */
    public void loadCachedListOfServers() {
        if (cacheFile != null && cacheFile.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(cacheFile);
                localSrvListCache.load(in);
                LOGGER.info("Load server list of " + clientName + " from local disk success");
            } catch (Throwable e) {
                LOGGER.warn("Load server list of " + clientName + " from local disk failed. ", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOGGER.warn("Close cache file input stream failed. ", e);
                    }
                }
            }
        }
    }

    /**
     * Get the server list from local disk
     *
     * @return
     */
    public List<Server> getCachedListOfServers() {
        List<Server> cachedServerList = new ArrayList<>();

        Set<Map.Entry<Object, Object>> cacheEntries = localSrvListCache.entrySet();
        if (cacheEntries.size() == 0) {
            LOGGER.info("Local cache dose not have any server when get server list from it.");
            return cachedServerList;
        }

        long getStartTime = System.currentTimeMillis();

        for (Map.Entry<Object, Object> entry : cacheEntries) {
            String serversClassName = (String) entry.getKey();
            String serverListJson = (String) entry.getValue();
            if (!StringUtils.isEmpty(serversClassName) && !StringUtils.isEmpty(serverListJson)) {
                try {
                    List cachedList = jsonToServerList(serverListJson, Class.forName(serversClassName));
                    cachedServerList.addAll(cachedList);
                    LOGGER.info("Get server list from local cache success, size {}", cachedServerList.size());
                    break;
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Get server list from local cache failed. ", e);
                    break;
                }
            }
        }
        long getCost = System.currentTimeMillis() - getStartTime;
        if (getCost > 2000) {
            LOGGER.info("Get cache list cost {}ms", getCost);
        }
        LOGGER.debug("Get cache list cost {}ms", getCost);
        return cachedServerList;
    }

    private <T> List<T> jsonToServerList(String json, Class<T> serverClass) {
        Type type = new TypeToken<ArrayList<JsonObject>>() {}.getType();
        ArrayList<JsonObject> jsonObjs = GSON.fromJson(json, type);

        List<T> serverList = new ArrayList<>();
        for (JsonObject jsonObj : jsonObjs) {
            serverList.add(GSON.fromJson(jsonObj, serverClass));
        }

        return serverList;
    }

    /**
     * Update the server list in mem cache.
     *
     * @param servers
     */
    public void updateCachedListOfServers(List<Server> servers) {
        if (servers == null || servers.size() == 0) {
            return;
        }
        long updateStartTime = System.currentTimeMillis();

        String serversClassName = servers.get(0).getClass().getName();
        String serverListJson = GSON.toJson(servers);
        // update mem
        localSrvListCache.setProperty(serversClassName, serverListJson);

        long updateCost = System.currentTimeMillis() - updateStartTime;
        if (updateCost > 2000) {
            LOGGER.info("Update CachedListOfServers cost {}ms", updateCost);
        }
        LOGGER.debug("Update CachedListOfServers cost {}ms", updateCost);
    }

    /**
     * Store server list to local disk synchronized 防止退出时更新本地文件与定时任务更新
     */
    public synchronized void storeCachedListOfServer() {
        if (cacheFile == null || !cacheFile.exists()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        // notice: 当前场景 均为单线程进行文件的存储和读取，不存在并发的问题
        try {
            try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                localSrvListCache.store(out, "Starlight Local Registry");
            }
        } catch (Throwable e) {
            LOGGER.warn("Store server list to local disk cache failed. ", e);
        }
        long storeCost = System.currentTimeMillis() - startTime;
        if (storeCost > 2000) {
            LOGGER.info("Store CachedListOfServers cost {}ms", storeCost);
        }
        LOGGER.debug("Store CachedListOfServers cost {}ms", storeCost);
    }

    /**
     * Close local store
     */
    public void close() {
        if (storeCacheExecutor != null) {
            storeCacheExecutor.shutdown();
        }

        storeCachedListOfServer();
    }

    private String getCacheFileName() {
        return System.getProperty("java.io.tmpdir") + "/starlight/local-registry/" + clientName + ".cache";
    }

    protected void createCacheFile(String cacheFilePath) {
        cacheFile = new File(cacheFilePath);
        if (!cacheFile.getParentFile().exists()) { // dir not exist create
            if (!cacheFile.getParentFile().mkdirs()) {
                LOGGER.warn("Invalid file cache path " + cacheFilePath + ", failed to create dirs "
                    + cacheFile.getParentFile());
            } else {
                try {
                    if (!cacheFile.exists()) {
                        cacheFile.createNewFile();
                    }
                } catch (IOException e) {
                    LOGGER.warn("Create new cache file failed when init local cache, cause by: {}", e.getMessage());
                }
            }
        } else {
            try {
                if (!cacheFile.exists()) {
                    cacheFile.createNewFile();
                }
            } catch (IOException e) {
                LOGGER.warn("Create new cache file failed when init local cache, cause by: {}", e.getMessage());
            }
        }
    }
}
