package com.baidu.brpc.example.dubbo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EchoResponse implements java.io.Serializable {
    private static final long serialVersionUID = -3450064362986273897L;

    private String message;
}
