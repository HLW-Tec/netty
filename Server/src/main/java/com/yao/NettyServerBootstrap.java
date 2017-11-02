package com.yao;

import com.yao.module.AskMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Created by yaozb on 15-4-11.
 */
public class NettyServerBootstrap {
    // 设置6秒检测chanel是否接受过心跳数据
    private static final int READ_WAIT_SECONDS = 3;
    private int port;
    private SocketChannel socketChannel;
    public NettyServerBootstrap(int port) throws InterruptedException {
        this.port = port;
        bind();
    }

    private void bind() throws InterruptedException {

        ServerBootstrap bootstrap=new ServerBootstrap();
        //绑定两个线程组分别用来处理客户端通道的accept和读写时间
        EventLoopGroup boss=new NioEventLoopGroup();
        EventLoopGroup worker=new NioEventLoopGroup();
        bootstrap.group(boss,worker);
        //绑定服务端通道NioServerSocketChannel
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        //给读写事件的线程通道绑定handler去真正的处理写
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline p = socketChannel.pipeline();
        // 使用ObjectDecoder和ObjectEncoder 因为双向都有写数据和读数据，所以这里需要两个都设置 如果只读，那么只需要ObjectDecoder即可
                p.addLast(new ObjectEncoder());
                p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
       //超时时间为3秒,写超时和读超时均为0;然后加上时间控制单元
                p.addLast(new IdleStateHandler(READ_WAIT_SECONDS, 0, 0, TimeUnit.SECONDS));
                p.addLast("NettyServerHandler",new NettyServerHandler());
            }
        });
        // 绑定端口，开始接收进来的连接
        ChannelFuture f= bootstrap.bind(port).sync();
        if(f.isSuccess()){
            System.out.println("server start---------------");
        }
    }
    public static void main(String []args) throws InterruptedException {
        NettyServerBootstrap bootstrap=new NettyServerBootstrap(9999);
        while (true){
            SocketChannel channel=(SocketChannel)NettyChannelMap.get("001");
            if(channel!=null){
                AskMsg askMsg=new AskMsg();
                channel.writeAndFlush(askMsg);
            }
            TimeUnit.SECONDS.sleep(1000);
        }
    }
}
