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
 
package com.baidu.cloud.starlight.api.utils;

import com.baidu.cloud.starlight.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by liuruisen on 2020/1/16.
 */
public class NetUriUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetUriUtils.class);

    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)");

    /**
     * The Constant IP_PATTERN.
     */
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    // cache
    private static InetAddress LOCAL_ADDRESS = null;

    private static String localHostIp = null;

    public static Map<String, String> parseQueryString(String qs) {
        if (qs == null || qs.length() == 0) {
            return new HashMap<String, String>();
        }
        return parseKeyValuePair(qs, "\\&");
    }

    private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
        String[] tmp = str.split(itemSeparator);
        Map<String, String> map = new HashMap<String, String>(tmp.length);
        for (int i = 0; i < tmp.length; i++) {
            Matcher matcher = KVP_PATTERN.matcher(tmp[i]);
            if (matcher.matches() == false) {
                continue;
            }
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static String toQueryString(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        if (ps != null && ps.size() > 0) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(ps).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && key.length() > 0 && value != null && value.length() > 0) {
                    if (buf.length() > 0) {
                        buf.append("&");
                    }
                    buf.append(key);
                    buf.append("=");
                    buf.append(value);
                }
            }
        }
        return buf.toString();
    }

    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static synchronized String getLocalHost() {
        // get from cache
        if (!StringUtils.isEmpty(localHostIp)) {
            return localHostIp;
        }

        // get from env
        if (EnvUtils.isJarvisEnv()) {
            if (!StringUtils.isEmpty(System.getenv(Constants.EM_IP))) {
                localHostIp = System.getenv(Constants.EM_IP);
                return localHostIp;
            }

            if (!StringUtils.isEmpty(System.getenv(Constants.MATRIX_HOST_IP))) {
                localHostIp = System.getenv(Constants.MATRIX_HOST_IP);
                return localHostIp;
            }
        }

        // get from NetworkInterface
        if (LOCAL_ADDRESS == null) {
            LOCAL_ADDRESS = getLocalAddress();
        }
        localHostIp = LOCAL_ADDRESS.getHostAddress();

        return localHostIp;
    }

    /**
     * Get local address Use real IP first, localhost second
     * 
     * @return
     */
    public static InetAddress getLocalAddress() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface network = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = network.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (isValidAddress(address)) {
                            return address;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to retrieve ip address.", e);
        }
        LOGGER.error("Failed to get local host ip address, use 127.0.0.1 instead.");
        return localAddress;
    }

    /**
     * check if is a valid address 1. not null 2. not loopbackip 3. not 0.0.0.0 or 127.0.0.1
     *
     * @param address
     * @return the address is valid or not
     */
    public static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }

        String addressName = address.getHostAddress();
        return (addressName != null && !addressName.equals(Constants.ANYHOST_VALUE)
            && !addressName.equals(Constants.LOCALHOST_VALUE) && IP_PATTERN.matcher(addressName).matches());
    }
}
