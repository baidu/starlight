package com.baidu.cloud.starlight.benchmark.serializer.protobuf.protostuff;


import com.baidu.cloud.starlight.benchmark.model.Address;
import com.baidu.cloud.starlight.benchmark.model.AddressAdd;
import com.baidu.cloud.starlight.benchmark.model.ExtInfo;
import com.baidu.cloud.starlight.benchmark.model.Sex;
import com.baidu.cloud.starlight.benchmark.model.SexAdd;
import com.baidu.cloud.starlight.benchmark.model.User;
import com.baidu.cloud.starlight.benchmark.model.UserAddFirst;
import com.baidu.cloud.starlight.benchmark.model.UserAddLast;
import com.baidu.cloud.starlight.benchmark.model.UserAddMiddle;
import com.baidu.cloud.starlight.benchmark.model.UserChangeType;
import com.baidu.cloud.starlight.benchmark.model.UserEnumAddValue;
import com.baidu.cloud.starlight.benchmark.model.UserParamAdd;

import java.util.Collections;

/**
 * Created by liuruisen on 2020/9/3.
 */
public class CompatibleApiVertify {


    private static byte[] NORMAL_BYTES;
    private static byte[] ADD_MIDDLE_BYTES;
    private static byte[] ADD_FIRST_BYTES;
    private static byte[] ENUM_ADD_BYTES;
    private static byte[] TYPE_CHANGE_BYTES;
    private static byte[] ADD_LAST_BYTES;
    private static byte[] PARAM_ADD_BYTES;

    static {
        try {
            NORMAL_BYTES = storeNormalBytes();
            ADD_MIDDLE_BYTES = storeAddMiddleBytes();
            ADD_FIRST_BYTES = storeAddFirstBytes();
            ENUM_ADD_BYTES = storeEnumAddBytes();
            TYPE_CHANGE_BYTES = storeTypeChangeBytes();
            ADD_LAST_BYTES = storeAddLastBytes();
            PARAM_ADD_BYTES = storeParamAddBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        try {
            User normal = (User) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES, User.class);
            System.out.println("Normal: " + normal + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            User enumAdd = (User) ProtoStuffSerializer.bodyDeserialize(ENUM_ADD_BYTES, User.class);
            System.out.println("EnumAdd: " + enumAdd + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize EnumAdd failed");
            e.printStackTrace();
        }

        try {
            User typeChange = (User) ProtoStuffSerializer.bodyDeserialize(TYPE_CHANGE_BYTES, User.class);
            System.out.println("TypeChange: " + typeChange + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize TypeChange failed");
            e.printStackTrace();
        }

        try {
            User addLast = (User) ProtoStuffSerializer.bodyDeserialize(ADD_LAST_BYTES, User.class);
            System.out.println("AddLast: " + addLast + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddLast failed");
            e.printStackTrace();
        }

        try {
            User addMiddle = (User) ProtoStuffSerializer.bodyDeserialize(ADD_MIDDLE_BYTES, User.class);
            System.out.println("AddMiddle: " + addMiddle + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddMiddle failed");
            e.printStackTrace();
        }

        try {
            User addFirst = (User) ProtoStuffSerializer.bodyDeserialize(ADD_FIRST_BYTES, User.class);
            System.out.println("AddFirst: " + addFirst + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddFirst failed");
            e.printStackTrace();
        }

        try {
            User paramAdd = (User) ProtoStuffSerializer.bodyDeserialize(PARAM_ADD_BYTES, User.class);
            System.out.println("ParamAdd: " + paramAdd + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize ParamAdd failed");
            e.printStackTrace();
        }


        try {
            User normalVerse = (User) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES, User.class);
            System.out.println("normalVerse: " + normalVerse + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            UserEnumAddValue enumAddVerse = (UserEnumAddValue) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES,
                    UserEnumAddValue.class);
            System.out.println("EnumAddVerse: " + enumAddVerse + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize EnumAddVerse failed");
            e.printStackTrace();
        }

        try {
            UserChangeType typeChangeVerse = (UserChangeType) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES, UserChangeType.class);
            System.out.println("TypeChangeVerse: " + typeChangeVerse + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize TypeChangeVerse failed");
            e.printStackTrace();
        }

        try {
            UserAddMiddle addMiddleVerse = (UserAddMiddle) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES, UserAddMiddle.class);
            System.out.println("AddMiddleVerse: " + addMiddleVerse + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddMiddleVerse failed");
            e.printStackTrace();
        }

        try {
            UserAddFirst addFirstVerse = (UserAddFirst) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES,
                    UserAddFirst.class);
            System.out.println("AddFirstVerse: " + addFirstVerse + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddFirstVerse failed");
            e.printStackTrace();
        }

        try {
            UserAddLast addLastVerse = (UserAddLast) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES,
                    UserAddLast.class);
            System.out.println("AddLastVerse: " + addLastVerse + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize AddLastVerse failed");
            e.printStackTrace();
        }

        try {
            UserParamAdd paramAdd = (UserParamAdd) ProtoStuffSerializer.bodyDeserialize(NORMAL_BYTES,
                    UserParamAdd.class);
            System.out.println("ParamAddVerse: " + paramAdd + "\n");
        } catch (Exception e) {
            System.out.println("Deserialize ParamAddVerse failed");
            e.printStackTrace();
        }

    }


    private static byte[] storeNormalBytes() throws Exception {
        User user = new User();
        user.setUserId(1l);
        user.setUserName("user1");
        user.setInfo("save message");
        user.setSex(Sex.FEMALE);
        user.setAge(18);
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, User.class);
    }

    private static byte[] storeAddMiddleBytes() throws Exception {

        UserAddMiddle user = new UserAddMiddle();
        user.setUserId(1l);
        user.setHobby("Basketball"); // add middle
        user.setUserName("user1");
        user.setInfo("save message");
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setSex(Sex.FEMALE);
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserAddMiddle.class);

    }


    private static byte[] storeAddFirstBytes() throws Exception {

        UserAddFirst user = new UserAddFirst();
        user.setHobby("Basketball"); // add first
        user.setUserId(1l);
        user.setUserName("user1");
        user.setInfo("save message");
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setSex(Sex.FEMALE);
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserAddFirst.class);

    }


    private static byte[] storeEnumAddBytes() throws Exception {

        UserEnumAddValue user = new UserEnumAddValue();
        user.setUserId(1l);
        user.setUserName("user1");
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setInfo("save message");
        user.setSex(SexAdd.OTHRE);
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserEnumAddValue.class);

    }

    private static byte[] storeTypeChangeBytes() throws Exception {

        UserChangeType user = new UserChangeType();
        user.setUserId("1");
        user.setUserName("user1");
        user.setInfo("save message");
        user.setSex(Sex.FEMALE);
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserChangeType.class);

    }

    private static byte[] storeAddLastBytes() throws Exception {

        UserAddLast user = new UserAddLast();
        user.setHobby("Basketball"); // add first
        user.setUserId(1l);
        user.setUserName("user1");
        user.setInfo("save message");
        user.setAddress(new Address("TTTTTTTTTT"));
        user.setSex(Sex.FEMALE);
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserAddLast.class);

    }

    private static byte[] storeParamAddBytes() throws Exception {
        UserParamAdd user = new UserParamAdd();
        user.setUserId(1l);
        user.setUserName("user1");
        user.setInfo("save message");
        AddressAdd addressAdd = new AddressAdd("TTTTTTTTTT");
        addressAdd.setInfo("info");
        user.setAddress(addressAdd);
        user.setSex(Sex.FEMALE);
        user.setAge(18);
        user.setBalance(12.22d);
        user.setSalary(10000.1f);
        user.setAlive(true);
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));
        user.setTags(Collections.singletonList("HHHHHHH"));
        ExtInfo extInfo = new ExtInfo();
        extInfo.setKey("Key");
        extInfo.setValue("Value");
        user.setExtInfos(Collections.singletonList(extInfo));
        user.setMap(Collections.singletonMap("key", new Address("Beijing")));

        return ProtoStuffSerializer.bodySerialize(user, UserParamAdd.class);
    }
}
