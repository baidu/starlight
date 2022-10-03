package com.baidu.cloud.starlight.benchmark.service;

import com.baidu.cloud.starlight.benchmark.model.ExtInfo;
import com.baidu.cloud.starlight.benchmark.model.User;


import java.util.LinkedList;
import java.util.List;

/**
 * Created by liuruisen on 2020/3/6.
 */
public class UserServiceImpl implements UserService {

    @Override
    public User getUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("User1");
        user.setBalance(1000.21d);
        List<String> tags = new LinkedList<>();
        tags.add("fgh");
        tags.add("123123");
        user.setTags(tags);
        List<ExtInfo> extInfos = new LinkedList<>();
        ExtInfo extInfo = new ExtInfo("hobby", "learn");
        extInfos.add(extInfo);
        user.setExtInfos(extInfos);
        return user;
    }

    @Override
    public User updateUser(User user) {
        return user;
    }

    @Override
    public void deleteUser(Long userId) {
        System.out.println("Delete user {" + userId + "}");
    }

    @Override
    public Long saveUser(User user) {
        return user.getUserId();
    }
}
