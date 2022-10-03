package com.baidu.cloud.starlight.benchmark.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by liuruisen on 2019/10/29.
 */
public class BenchMarkUtil {

    public static synchronized byte[] initInputMessageBytes(String messageTxt) {

        byte[] messageBytes = null;
        try (InputStream inputStream = Thread.currentThread().getClass().getResourceAsStream(messageTxt)) {
            int messageLength = inputStream.available();
            messageBytes = new byte[messageLength];
            inputStream.read(messageBytes);
        } catch (IOException ex) {
            System.exit(-1);
        }

        return messageBytes;

    }


    public static synchronized void writeIntoFile(String frameName, byte[] messageBytes) {

        try (FileOutputStream fos = new FileOutputStream(frameName)) {
            fos.write(messageBytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}