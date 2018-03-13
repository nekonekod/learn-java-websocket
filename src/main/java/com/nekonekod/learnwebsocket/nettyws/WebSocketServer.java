package com.nekonekod.learnwebsocket.nettyws;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Author: duwenjun
 * Date: 2018/03/13
 * Time: 上午10:35
 * Project: learn-websocket
 */
@Log4j2
public class WebSocketServer extends TimerTask implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * 处理客户端连接请求
     */
    @Resource
    private EventLoopGroup bossGroup;

    /**
     * 处理客户端IO操作
     */
    @Resource
    private EventLoopGroup workerGroup;


    private ChannelHandler childChannelHandler;

    private ChannelFuture channelFuture;

    private int port;

    /**
     * netty 启动服务类
     */
    @Resource
    private ServerBootstrap serverBootstrap;

    @Override
    public void run() {
        build(this.port);
        log.info("netty WebSocketServer start...");
    }

    private void build(int port) {
        try {
            //(1)boss用于处理客户端的tcp连接请求  worker负责与客户端之前的读写操作
            //(2)配置客户端的channel类型
            //(3)配置TCP参数，握手字符串长度设置
            //(4)TCP_NODELAY是一种算法，为了充分利用带宽，尽可能发送大块数据，减少充斥的小块数据，true是关闭，可以保持高实时性,若开启，减少交互次数，但是时效性相对无法保证
            //(5)开启心跳包活机制，就是客户端、服务端建立连接处于ESTABLISHED状态，超过2小时没有交流，机制会被启动
            //(6)netty提供了2种接受缓存区分配器，FixedRecvByteBufAllocator是固定长度，但是拓展，AdaptiveRecvByteBufAllocator动态长度
            //(7)绑定I/O事件的处理类,WebSocketChildChannelHandler中定义
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(592048))
                    .childHandler(childChannelHandler);
            channelFuture = serverBootstrap.bind(port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            //init
            new Timer().schedule(this, 0);
        }
    }

    //执行之后关闭
    @PreDestroy
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public ChannelHandler getChildChannelHandler() {
        return childChannelHandler;
    }

    public void setChildChannelHandler(ChannelHandler childChannelHandler) {
        this.childChannelHandler = childChannelHandler;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
