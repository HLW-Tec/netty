package com.yao;

import com.yao.module.AskMsg;
import com.yao.module.AskParams;
import com.yao.module.Constants;
import com.yao.module.LoginMsg;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

/**
 * Created by yaozb on 15-4-11.
 */
public class NettyClientBootstrap {
    private int port;
    private String host;
    private SocketChannel socketChannel;
    private ChannelFuture future;
    private static final EventExecutorGroup group = new DefaultEventExecutorGroup(20);
    public NettyClientBootstrap(int port, String host) throws InterruptedException {
        this.port = port;
        this.host = host;
        start();
    }
    private void start() throws InterruptedException {
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true);
        bootstrap.group(eventLoopGroup);
        bootstrap.remoteAddress(host,port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(new IdleStateHandler(3,5,0,TimeUnit.SECONDS));
                pipeline.addLast(new ObjectEncoder());
                pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                pipeline.addLast(new NettyClientHandler());
            }
        });
            future = bootstrap.connect(host,port).sync();
        if (future.isSuccess()) {
            socketChannel = (SocketChannel)future.channel();
            System.out.println("connect server  成功---------");
        }
    }
    public static void main(String[]args) throws InterruptedException {
        Constants.setClientId("001");
        NettyClientBootstrap bootstrap=new NettyClientBootstrap(9999,"localhost");

        LoginMsg loginMsg=new LoginMsg();
        loginMsg.setPassword("yao");
        loginMsg.setUserName("robin");
        bootstrap.socketChannel.writeAndFlush(loginMsg);
        while (true){
            TimeUnit.SECONDS.sleep(6);
            AskMsg askMsg=new AskMsg();
            AskParams askParams=new AskParams();
            askParams.setAuth("authToken");
            askMsg.setParams(askParams);
            bootstrap.socketChannel.writeAndFlush(askMsg);
        }
    }
}
