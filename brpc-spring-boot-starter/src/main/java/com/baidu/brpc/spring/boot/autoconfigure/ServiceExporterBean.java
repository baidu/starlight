package com.baidu.brpc.spring.boot.autoconfigure;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServiceExporterBean {
    private Class<?> serviceInterface;
    private Object serviceBean;
}
