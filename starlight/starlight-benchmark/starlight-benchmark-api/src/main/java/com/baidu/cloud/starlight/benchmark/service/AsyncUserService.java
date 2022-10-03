package com.baidu.cloud.starlight.benchmark.service;

import com.baidu.cloud.starlight.benchmark.model.User;
import com.baidu.cloud.starlight.api.rpc.callback.Callback;

import java.util.concurrent.Future;

/**
 * Created by liuruisen on 2020/3/11.
 */
public interface AsyncUserService extends UserService {

    void getUserCallback(Long userId, Callback callback);

    Future<User> getUserFuture(Long userId);

    void updateUserCallback(User user, Callback callback);
}
