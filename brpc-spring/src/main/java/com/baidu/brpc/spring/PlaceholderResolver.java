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
package com.baidu.brpc.spring;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 字符串占位符解析工作类.<br>
 * 例如: String testStr = "abc${ab}" 如果ab="ddd",则解析后结果为 abcddd<br>
 * <pre>
 * example:
 *     public static void main(String[] args) {
        final Map<String, String> placeholderVals = new HashMap<String, String>(5);
        placeholderVals.put("key1", "china");
        placeholderVals.put("key2", "3");
        placeholderVals.put("key3", "beijin");
        
        String testStr = "hello ${key1}";
        
        PlaceholderResolver resolver = new PlaceholderResolver(new PlaceholderResolved() {
            
            public String doResolved(String placeholder) {
                System.out.println("find placeholder:" + placeholder);
                return placeholderVals.get(placeholder);
            }
        });
        resolver.setPlaceholderPrefix("${");
        resolver.setPlaceholderSuffix("}");
        
        System.out.println(resolver.doParse(testStr));
        
        testStr = "hello ${key${key2}}";
        System.out.println(resolver.doParse(testStr));
    }
    </pre>
 * 
 * @author xiemalin
 * @since 2.17
 */
public class PlaceholderResolver {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(PlaceholderResolver.class.getName());

    /**  Default placeholder prefix: "${". */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /**  Default placeholder suffix: "}". */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /** placeholder prefix. */
    private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    /** placeholder suffix. */
    private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    /**
     * {@link PlaceholderResolved} instance.
     */
    private PlaceholderResolved resolvedInterceptor;
    
    /** all parsed placeholders will store here. */
    private Set<String> visitedPlaceholders = new HashSet<String>(50);

    /**
     * Constructor method.
     *
     * @param resolvedInterceptor {@link PlaceholderResolved} can not be null.
     */
    public PlaceholderResolver(
            PlaceholderResolved resolvedInterceptor) {
        if (resolvedInterceptor == null) {
            throw new IllegalArgumentException(
                    "property 'resolvedInterceptor' is null");
        }
        this.resolvedInterceptor = resolvedInterceptor;
    }
    
    /**
     * Do parser placeholder action.
     * 
     * 
     * @param strVal target string to parser
     * @return target string after placeholder parse
     */
    public String doParse(String strVal) {
        if (strVal == null) {
            return strVal;
        }
        return parseStringValue(strVal, visitedPlaceholders);
    }
    
    /**
     * test if target string contains placeholderPrefix.
     *
     * @param strVal target string to test
     * @return true if string contains placeholderPrefix
     */
    public boolean hasPlaceHolder(String strVal) {
        if (StringUtils.isBlank(strVal)) {
            return false;
        }
        int startIndex = strVal.indexOf(this.placeholderPrefix);
        if (startIndex == -1) {
            return false;
        }
        return true;
    }

    /**
     * Parse the given String value recursively, to be able to resolve nested
     * placeholders (when resolved property values in turn contain placeholders
     * again).
     *
     * @param strVal            the String value to parse
     * @param visitedPlaceholders the placeholders that have already been visited
     * during the current resolution attempt (used to detect circular references
     * between placeholders). Only non-null if we're parsing a nested placeholder.
     * @return the string
     */
    protected String parseStringValue(String strVal,
            Set<String> visitedPlaceholders) {

        StringBuilder buf = new StringBuilder(strVal);

        int startIndex = strVal.indexOf(this.placeholderPrefix);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex
                        + this.placeholderPrefix.length(), endIndex);
                if (!visitedPlaceholders.add(placeholder)) {
                    throw new RuntimeException(
                            "Circular placeholder reference '" + placeholder
                                    + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the
                // placeholder key.
                placeholder = parseStringValue(placeholder, visitedPlaceholders);
                // Now obtain the value for the fully resolved key...
                String propVal = resolvedInterceptor.doResolved(placeholder);
                
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, visitedPlaceholders);
                    buf.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                        
                    LOGGER.log(Level.FINEST, "Resolved placeholder '" + placeholder + "'");
                    startIndex = buf.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                }
                else {
                    startIndex = buf.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                }
                visitedPlaceholders.remove(placeholder);
            }
            else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }
    
    /**
     * To find placeholder position from index.
     * 
     * @param buf target string
     * @param startIndex start index
     * @return -1 if not found or return position index.
     */ 
    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                }
                else {
                    return index;
                }
            }
            else if (substringMatch(buf, index, this.placeholderPrefix)) {
                withinNestedPlaceholder++;
                index = index + this.placeholderPrefix.length();
            }
            else {
                index++;
            }
        }
        return -1;
    }    

    /**
     * Test whether the given string matches the given substring
     * at the given index.
     *
     * @param str the original string (or StringBuffer)
     * @param index the index in the original string to start matching against
     * @param substring the substring to match at the given index
     * @return true, if successful
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the placeholder prefix.
     *
     * @param placeholderPrefix the new placeholder prefix
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * Sets the placeholder suffix.
     *
     * @param placeholderSuffix the new placeholder suffix
     */
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }
    
}
