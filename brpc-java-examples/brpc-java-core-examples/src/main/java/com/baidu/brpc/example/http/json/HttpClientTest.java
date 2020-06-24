package com.baidu.brpc.example.http.json;

import java.io.IOException;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.baidu.brpc.utils.GsonUtils;

/**
 * created by wangsan on 2020/6/24.
 *
 * @author wangsan
 */
public class HttpClientTest {
    public static void main(String[] args) {
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setSocketTimeout(5000)
                                .setConnectTimeout(5000)
                                .setConnectionRequestTimeout(5000)
                                .build()
                )
                .build();
        String url = "http://127.0.0.1:8080/test/echo/hello3";
        HttpPost post = new HttpPost(url);

        Echo req = new Echo("foo", new Date());
        String requestBody = GsonUtils.toJson(req);
        requestBody = "{\"message\":\"hello foo\",\"date\":\"2020-06-24wrong 10:08:17\"}";
        post.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        post.addHeader("Accept", ContentType.APPLICATION_JSON.toString());

        try {
            CloseableHttpResponse response = client.execute(post);
            System.out.println(response.getStatusLine());
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
