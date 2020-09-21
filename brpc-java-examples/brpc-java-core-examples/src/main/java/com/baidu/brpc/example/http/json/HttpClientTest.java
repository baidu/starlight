package com.baidu.brpc.example.http.json;

import java.util.Date;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.baidu.brpc.utils.GsonUtils;

/**
 * created by wangsan on 2020/6/24.
 *
 * @author wangsan
 */
public class HttpClientTest {
    public static void main(String[] args) {
        CloseableHttpClient client = HttpClients.createDefault();
        // 支持多级path，但method部分只取最后一部分
        String url = "http://127.0.0.1:8080/test/echo/hello3";

        // right data
        Echo req = new Echo("foo", new Date());
        String requestBody = GsonUtils.toJson(req);
        post(client, url, requestBody);

        // wrong date,return {"message":"hello foo","time":null}
        post(client, url, "{\"message\":\"foo\",\"date\":\"2020-06-24wrong 10:08:17\"}");

        // wrong json, return HTTP/1.1 400 Bad Request
        post(client, url, "wrong-body");
    }

    private static void post(CloseableHttpClient client, String url, String body) {
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        post.addHeader("Accept", ContentType.APPLICATION_JSON.toString());
        try {
            CloseableHttpResponse response = client.execute(post);
            System.out.println(response.getStatusLine());
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
