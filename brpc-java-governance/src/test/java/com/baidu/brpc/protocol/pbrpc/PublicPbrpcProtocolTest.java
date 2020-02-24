package com.baidu.brpc.protocol.pbrpc;

import com.baidu.brpc.ProtobufRpcMethodInfo;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.RpcRequest;
import com.baidu.brpc.protocol.RpcResponse;
import com.baidu.brpc.protocol.nshead.NSHead;
import com.baidu.brpc.protocol.standard.Echo.EchoRequest;
import com.baidu.brpc.protocol.standard.Echo.EchoResponse;
import com.baidu.brpc.server.ServiceManager;
import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PublicPbrpcProtocolTest {

    private PublicPbrpcProtocol protocol = new PublicPbrpcProtocol();

    @Before
    public void init() {
        if (ServiceManager.getInstance() != null) {
            ServiceManager.getInstance().getServiceMap().clear();
        }
    }


    @Test
    public void testEncodeRequest() throws Exception {
        EchoRequest request = buildRequest();

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setArgs(new Object[] {request});
        rpcRequest.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        rpcRequest.setLogId(3L);
        rpcRequest.setNsHead(new NSHead((int) rpcRequest.getLogId(), 0));
        rpcRequest.setTargetMethod(EchoService.class.getMethods()[0]);
        rpcRequest.setServiceName("EchoService");
        ByteBuf byteBuf = protocol.encodeRequest(rpcRequest);
        NSHead nsHead = NSHead.fromByteBuf(byteBuf);

        assertEquals(3, nsHead.logId);
        assertEquals(byteBuf.readableBytes(), nsHead.bodyLength);
    }

    @Test
    public void testEncodeResponse() throws Exception {

        EchoResponse result = buildResponse();

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setLogId(4L);
        rpcResponse.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));
        rpcResponse.setResult(result);

        Request rpcRequest = new RpcRequest();
        rpcRequest.setLogId(4L);
        rpcRequest.setRpcMethodInfo(new ProtobufRpcMethodInfo(EchoService.class.getMethods()[0]));

        ByteBuf byteBuf = protocol.encodeResponse(rpcRequest, rpcResponse);
        NSHead nsHead = NSHead.fromByteBuf(byteBuf);

        assertEquals(4, nsHead.logId);
        assertEquals(byteBuf.readableBytes(), nsHead.bodyLength);
    }


    public EchoRequest buildRequest() {

        EchoRequest.Builder builder = EchoRequest.newBuilder();
        builder.setMessage("hello world");
        return builder.build();
    }

    public EchoResponse buildResponse() {

        EchoResponse.Builder builder = EchoResponse.newBuilder();
        builder.setMessage("sync!");
        return builder.build();

    }
}
