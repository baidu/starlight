package com.baidu.brpc.server.push;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response {
    public int code;
    public String message;

    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Response success() {
        return new Response(0, null);
    }
}
