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

package com.baidu.brpc.exceptions;

public class NotEnoughDataException extends Exception {
    private int code;

    public NotEnoughDataException() {
        super();
    }

    public NotEnoughDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughDataException(String message) {
        super(message);
    }

    public NotEnoughDataException(Throwable cause) {
        super(cause);
    }

    public NotEnoughDataException(int code) {
        super();
        this.code = code;
    }

    public NotEnoughDataException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public NotEnoughDataException(int code, String message) {
        super(message);
        this.code = code;
    }

    public NotEnoughDataException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    /**
     * avoid the expensive and useless stack trace for exceptions
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}