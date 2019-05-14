package com.baidu.brpc.utils;


import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

public class GsonUtilsTest {

    @Test
    public void fromJson() {
        Date date = new Date();
        Date d1 = GsonUtils.fromJson(GsonUtils.toJson(date), new TypeToken<Date>(){}.getType());
        Date d2 = GsonUtils.fromJson(GsonUtils.toJson(date), new TypeToken<java.sql.Date>(){}.getType());

        date = new java.sql.Date(System.currentTimeMillis());
        d1 = GsonUtils.fromJson(GsonUtils.toJson(date), new TypeToken<Date>(){}.getType());
        d2 = GsonUtils.fromJson(GsonUtils.toJson(date), new TypeToken<java.sql.Date>(){}.getType());

        java.sql.Date sDate = new java.sql.Date(System.currentTimeMillis());
        java.sql.Date d3 = GsonUtils.fromJson(GsonUtils.toJson(date), new TypeToken<java.sql.Date>(){}.getType());


    }
}