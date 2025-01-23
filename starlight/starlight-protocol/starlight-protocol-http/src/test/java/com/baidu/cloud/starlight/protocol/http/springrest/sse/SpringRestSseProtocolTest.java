package com.baidu.cloud.starlight.protocol.http.springrest.sse;

import com.baidu.cloud.starlight.api.model.MsgBase;
import com.baidu.cloud.starlight.api.model.Response;
import com.baidu.cloud.starlight.api.transport.buffer.DynamicCompositeByteBuf;
import com.baidu.cloud.starlight.api.transport.channel.ChannelAttribute;
import com.baidu.cloud.starlight.api.transport.channel.ChannelSide;
import com.baidu.cloud.starlight.api.transport.channel.RpcChannel;
import com.baidu.cloud.starlight.api.transport.channel.ThreadLocalChannelContext;
import com.baidu.cloud.starlight.protocol.http.AbstractHttpProtocol;
import com.baidu.cloud.thirdparty.netty.buffer.ByteBuf;
import com.baidu.cloud.thirdparty.netty.buffer.Unpooled;
import com.baidu.cloud.thirdparty.netty.channel.embedded.EmbeddedChannel;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultHttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.DefaultLastHttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpContent;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpHeaderNames;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpObject;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponse;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseEncoder;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpResponseStatus;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.HttpVersion;
import com.baidu.cloud.thirdparty.netty.handler.codec.http.LastHttpContent;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.baidu.cloud.starlight.api.common.Constants.PROTOCOL_KEY;
import static com.baidu.cloud.starlight.api.common.Constants.SSE_EMBEDDED_CHANNEL_KEY;
import static com.baidu.cloud.starlight.api.common.Constants.SSE_REQUEST_ID_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringRestSseProtocolTest {

    @Test
    public void test1() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new HttpResponseEncoder());
        // part1
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        httpResponse.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        httpResponse.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        httpResponse.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        // request-id
        httpResponse.headers().set(AbstractHttpProtocol.X_STARLIGHT_ID, 1);
        // 适配零信任网关SSE逻辑，参考文档：https://ku.baidu-int.com/knowledge/HFVrC7hq1Q/b_dB7xLNHi/yToZib1hj4/hZlgASbuCxSw11
        httpResponse.headers().set("X-Accel-Buffering", "no");

        // part2
        String data = "data:{\"status\":200,\"data\":{\"sessionId\":\"AIX-618e2f8e-8e96-4076-911c-9054ee476b2a\",\"answer\":{\"content\":\"为您找到全部营销方案。\",\"cards\":[],\"instructions\":[{\"type\":202,\"payload\":{\"campaignParams\":{\"orderby\":\"addTime\",\"rowFilters\":[],\"limit\":[500],\"desc\":true},\"reportParams\":{\"token\":\"409f4eaa-ad19-464f-ac33-ab8b480asdds\",\"reportType\":1900001,\"userIds\":[630152],\"startDate\":\"2024-05-14\",\"endDate\":\"2024-05-14\",\"timeUnit\":\"SUMMARY\",\"columns\":[\"userId\",\"campaignNameStatus\",\"campaignId\",\"click\",\"ocpcTargetTrans\",\"deepConversions\",\"cost\",\"impression\",\"aixOcpcConversionsDetail28CVR\",\"aixOcpcConversionsDetail29CVR\"],\"startRow\":0,\"rowCount\":200,\"needSum\":true,\"needCache\":false,\"addZeroRows\":false,\"withColumnMeta\":false,\"topCount\":0},\"distribution\":\"space-between\",\"instructionParameters\":{\"reportParams\":{\"token\":\"409f4eaa-ad19-464f-ac33-ab8b480asdds\",\"reportType\":1900040,\"userIds\":[630152],\"startDate\":\"2024-05-14\",\"endDate\":\"2024-05-14\",\"timeUnit\":\"SUMMARY\",\"columns\":[\"userId\",\"projectNameStatus\",\"projectId\",\"click\",\"ocpcTargetTrans\",\"deepConversions\",\"cost\",\"impression\",\"aixOcpcConversionsDetail28CVR\",\"aixOcpcConversionsDetail29CVR\"],\"startRow\":0,\"rowCount\":200,\"needSum\":true,\"needCache\":false,\"addZeroRows\":false,\"withColumnMeta\":false,\"topCount\":0},\"focusOn\":\"Campaign\",\"projectParams\":{\"projectIds\":[]}},\"failInfo\":false}},{\"type\":206,\"payload\":{\"distribution\":\"space-between\",\"failInfo\":false}}],\"sugs\":[{\"content\":\"诊断账户\",\"prompt\":\"诊断账户\",\"api\":\"getNewDiagnosisInfo\",\"path\":\"aurora/GET/AixDiagnosisService/getNewDiagnosisInfo\",\"params\":\"{\\\"ids\\\":[630152],\\\"idType\\\":2,\\\"caseLevel\\\":[2,3]}\"},{\"content\":\"修改产品/服务描述\",\"prompt\":\"修改产品/服务描述\"},{\"content\":\"修改重点人群描述\",\"prompt\":\"修改重点人群描述\"},{\"content\":\"修改目标转化成本\",\"prompt\":\"修改目标转化成本\"},{\"content\":\"修改预算\",\"prompt\":\"修改预算\"},{\"content\":\"查看创意素材\",\"prompt\":\"查看创意素材\"}]},\"done\":true,\"scene\":3,\"tag\":0,\"agentContext\":{\"logInfo\":{\"raw\":[{\"intentType\":\"GET_CAMPAIGN\",\"operate\":\"GET\"}],\"rewrite\":{\"intentTypes\":[\"GET_CAMPAIGN\"],\"operate\":\"GET\"}},\"scene\":3}}}\n" +
                "\n";
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data.getBytes(StandardCharsets.UTF_8));
        HttpContent httpContent = new DefaultHttpContent(byteBuf);

        // part3
        LastHttpContent lastHttpContent = new DefaultLastHttpContent();

        embeddedChannel.writeOutbound(httpResponse, httpContent, lastHttpContent);

        ByteBuf[] outputBufs = new ByteBuf[embeddedChannel.outboundMessages().size()];
        embeddedChannel.outboundMessages().toArray(outputBufs);

        // 构造了一份response的数据块儿
        ByteBuf outboundByteBuf = Unpooled.wrappedBuffer(outputBufs);


        // 把数据块儿分为 多份
        int count = 5;
        int size_per_count = outboundByteBuf.readableBytes() / count;

        List<ByteBuf> byteBufList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                // 最后一份 取剩下所有的
                byteBufList.add(
                        outboundByteBuf.copy(i * size_per_count, outboundByteBuf.readableBytes() - i * size_per_count));
            } else {
                byteBufList.add(outboundByteBuf.copy(i * size_per_count, size_per_count));
            }
        }
        // 分块儿完成

        EmbeddedChannel channel = new EmbeddedChannel(new HttpSseResponseDecoderAdaptor());

        RpcChannel rpcChannel = mock(RpcChannel.class);
        when(rpcChannel.side()).thenReturn(ChannelSide.CLIENT);
        when(rpcChannel.getAttribute(SSE_EMBEDDED_CHANNEL_KEY)).thenReturn(channel);
        when(rpcChannel.getAttribute(SSE_REQUEST_ID_KEY)).thenReturn(-1L);
        when(rpcChannel.getAttribute(PROTOCOL_KEY)).thenReturn(SpringRestSseProtocol.PROTOCOL_NAME);

        ChannelAttribute attribute = new ChannelAttribute(rpcChannel);
        channel.attr(RpcChannel.ATTRIBUTE_KEY).set(attribute);
        ThreadLocalChannelContext.getContext().setChannel(channel);


        List<Response> msgs = new ArrayList<>();
        DynamicCompositeByteBuf input = new DynamicCompositeByteBuf();
        SpringRestSseHttpDecoder springRestSseHttpDecoder = new SpringRestSseHttpDecoder();
        for (ByteBuf buf : byteBufList) {
            input.addBuffer(buf.retain());
            try {
                MsgBase msg = springRestSseHttpDecoder.decode(input);
                if (msg != null) {
                    msgs.add((Response) msg);
                }
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        }



        List<HttpObject> httpObjects = msgs.stream().flatMap(response -> ((List<HttpObject>) response.getResult()).stream())
                .collect(Collectors.toList());

        Assert.assertTrue(httpObjects.size() == 3);
        Assert.assertTrue(httpObjects.get(0) instanceof HttpResponse);
        Assert.assertTrue(httpObjects.get(1) instanceof HttpContent);
        Assert.assertTrue(httpObjects.get(2) instanceof LastHttpContent);

    }
}
