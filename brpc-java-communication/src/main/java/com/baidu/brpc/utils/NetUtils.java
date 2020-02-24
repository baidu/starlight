/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
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
package com.baidu.brpc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Random;
import java.util.regex.Pattern;


/**
 * Utiltiy class for net.
 * 
 * @author xiemalin
 * @since 2.27
 */
public class NetUtils {

    /** The Constant logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NetUtils.class);

    /** The Constant LOCALHOST. */
    public static final String LOCALHOST = "127.0.0.1";

    /** The Constant ANYHOST. */
    public static final String ANYHOST = "0.0.0.0";

    /** The Constant RND_PORT_START. */
    private static final int RND_PORT_START = 30000;

    /** The Constant RND_PORT_RANGE. */
    private static final int RND_PORT_RANGE = 10000;

    /** The Constant RANDOM. */
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    
    /** The local address. */
    private static volatile InetAddress LOCAL_ADDRESS = null;

    /**
     * Gets the random port.
     *
     * @return the random port
     */
    public static int getRandomPort() {
        return RND_PORT_START + RANDOM.nextInt(RND_PORT_RANGE);
    }

    /**
     * Gets the available port.
     *
     * @return the available port
     */
    public static int getAvailablePort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Gets the available port.
     *
     * @param port the port
     * @return the available port
     */
    public static int getAvailablePort(int port) {
        if (port <= 0) {
            return getAvailablePort();
        }
        for (int i = port; i < MAX_PORT; i++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(i);
                return i;
            } catch (IOException e) {
                // continue
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return port;
    }

    /** The Constant MIN_PORT. */
    private static final int MIN_PORT = 0;

    /** The Constant MAX_PORT. */
    private static final int MAX_PORT = 65535;

    /**
     * Checks if is invalid port.
     *
     * @param port the port
     * @return true, if is invalid port
     */
    public static boolean isInvalidPort(int port) {
        return port > MIN_PORT || port <= MAX_PORT;
    }

    /** The Constant ADDRESS_PATTERN. */
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");

    /**
     * Checks if is valid address.
     *
     * @param address the address
     * @return true, if is valid address
     */
    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    /** The Constant LOCAL_IP_PATTERN. */
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    /**
     * Checks if is local host.
     *
     * @param host the host
     * @return true, if is local host
     */
    public static boolean isLocalHost(String host) {
        return host != null && (LOCAL_IP_PATTERN.matcher(host).matches() || host.equalsIgnoreCase("localhost"));
    }

    /**
     * Checks if is any host.
     *
     * @param host the host
     * @return true, if is any host
     */
    public static boolean isAnyHost(String host) {
        return "0.0.0.0".equals(host);
    }

    /**
     * Checks if is invalid local host.
     *
     * @param host the host
     * @return true, if is invalid local host
     */
    public static boolean isInvalidLocalHost(String host) {
        return host == null || host.length() == 0 || host.equalsIgnoreCase("localhost") || host.equals("0.0.0.0")
                || (LOCAL_IP_PATTERN.matcher(host).matches());
    }

    /**
     * Checks if is valid local host.
     *
     * @param host the host
     * @return true, if is valid local host
     */
    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    /**
     * Gets the local socket address.
     *
     * @param host the host
     * @param port the port
     * @return the local socket address
     */
    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
    }

    /** The Constant IP_PATTERN. */
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    /**
     * Checks if is valid address.
     *
     * @param address the address
     * @return true, if is valid address
     */
    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return (name != null && !ANYHOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
    }

    /**
     * Gets the local address.
     *
     * @return the local address
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null)
            return LOCAL_ADDRESS;
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    /**
     * Gets the log host.
     *
     * @return the log host
     */
    public static String getLogHost() {
        InetAddress address = LOCAL_ADDRESS;
        return address == null ? LOCALHOST : address.getHostAddress();
    }

    /**
     * Gets the local address0.
     *
     * @return the local address0
     */
    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to retrieve ip address: {}", e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable e) {
                                    LOGGER.warn("Failed to retrieve ip address: {}", e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.warn("Failed to retrieve ip address: {}", e);
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to retrieve ip address: {}", e);
        }
        LOGGER.error("Failed to get local host ip address, use 127.0.0.1 instead.");
        return localAddress;
    }
}
