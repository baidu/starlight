package com.baidu.cloud.starlight.benchmark.service;


import com.baidu.cloud.starlight.benchmark.model.User;

/**
 * Created by liuruisen on 2020/3/6.
 */
public interface UserService {

    User getUser(Long userId);

    User updateUser(User user);

    void deleteUser(Long userId);

    Long saveUser(User user);
}
