package com.baidu.brpc.example.dubbo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EchoRequest implements java.io.Serializable {
    private static final long serialVersionUID = -3450064362986273896L;

    private String message;
}
