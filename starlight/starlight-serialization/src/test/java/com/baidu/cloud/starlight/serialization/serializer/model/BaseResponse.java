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
 
package com.baidu.cloud.starlight.serialization.serializer.model;

/**
 * Created by liuruisen on 2021/2/4.
 */
public class BaseResponse {

    public static class LegoReturnCode {
        public static final int MAX_CODE = 100;
        public static final int SUCCESS = 0;
        public static final int INVALID_PARAMETER = 1;
        public static final int UNKNOWN_FAILURE = 2;
        public static final int NO_AUTHORITY = 3;
        public static final int TEMPLATE_NOT_RELEASED = 4;
        public static final int DEPENDENT_MODULE_FAILURE = 5;
        public static final int UPLOAD_FILE_FAILURE = 6;
        public static final int VALIDATE_FAILURE = 7;
        public static final int PARTIAL_SUCCESS = 8;
        public static final int TEMPLATE_JS_ERROR = 9;
        public static final int NO_NEW_TEMPLATE_VERSION = 10;

        public static final String[] RETRUN_MSG = new String[MAX_CODE];
        static {
            RETRUN_MSG[SUCCESS] = "success";
            RETRUN_MSG[INVALID_PARAMETER] = "invalid parameter";
            RETRUN_MSG[UNKNOWN_FAILURE] = "unknown failure";
            RETRUN_MSG[NO_AUTHORITY] = "no authority";
            RETRUN_MSG[TEMPLATE_NOT_RELEASED] = "template not released";
            RETRUN_MSG[DEPENDENT_MODULE_FAILURE] = "lego's dependent module failure";
            RETRUN_MSG[UPLOAD_FILE_FAILURE] = "upload file failure";
            RETRUN_MSG[VALIDATE_FAILURE] = "vaildate failure";
            RETRUN_MSG[PARTIAL_SUCCESS] = "partial success";
            RETRUN_MSG[TEMPLATE_JS_ERROR] = "template js error";
            RETRUN_MSG[NO_NEW_TEMPLATE_VERSION] = "can not convert material without new version";
        }
    }

    private int returnCode;
    private String returnMessage;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }
}
