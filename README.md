# Java Websocket
介绍三种websocket的java实现方式：原生+tomcat、基于spring、基于netty
## use default + tomcat
使用javax提供的注解实线原生+tomcat实现websocket  
相关注解：
* @ServerEndpoint(value = "/defaultws") 发布的路径，使用 ws://127.0.0.1:8080/defaultws 访问
* @OnOpen 连接时执行
* @OnClose 关闭时执行
* @OnMessage 收到消息时执行
* @OnError 连接错误时执行

```java
package com.nekonekod.learnwebsocket.defaultjava;

import lombok.extern.log4j.Log4j2;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: duwenjun
 * Date: 2018/03/05
 * Time: 下午2:07
 *
 * 
 */
@Log4j2
@ServerEndpoint(value = "/defaultws")
public class DefaultWebSocketHandler {

    private static ConcurrentHashMap<String, Session> map = new ConcurrentHashMap<>();

    //连接时执行
    @OnOpen
    public void onOpen(Session session) throws IOException {
        map.put(session.getId(), session);
        log.debug("新连接：{}", session.getId());
    }

    //关闭时执行
    @OnClose
    public void onClose(Session session) {
        map.remove(session.getId());
        log.debug("连接：{} 关闭", session.getId());
    }

    //收到消息时执行
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        log.debug("收到用户{}的消息{}：", session.getId(), message);
        map.values().forEach(s -> {
            try {
                s.getBasicRemote().sendText("[default]" + session.getId() + "：" + message); //群发
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    //连接错误时执行
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户id为：{}的连接发送错误", session.getId(), error);
    }

}
```

## use spring
### 处理类 SpringWebSocketHandler
编写SpringWebSocketHandler处理类，实现WebSocketHandler接口  
这个类处理websocket连接和处理消息入口  
重写以下方法：
* afterConnectionEstablished 连接建立后
* handleMessage 处理消息
* handleTransportError 处理异常
* afterConnectionClosed 连接关闭后
* supportsPartialMessages 是否支持分块的消息，如果返回true，要结合WebSocketMessage#isLast方法处理消息
```java
package com.nekonekod.learnwebsocket.springws;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: duwenjun
 * Date: 2018/03/12
 * Time: 下午5:36
 * Project: learn-websocket
 *
 * 
 */
@Log4j2
@Component
public class SpringWebSocketHandler implements WebSocketHandler {

    private Map<String, WebSocketSession> sessionHolder = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionHolder.put(session.getId(), session);
        log.info("afterConnectionEstablished:id={}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String msg = message.getPayload().toString();
        log.info("handleMessage:id={},message={}", session.getId(), msg);
        TextMessage textMessage = new TextMessage("[spring]" + session.getId() + " : " + msg);
        sessionHolder.forEach((id, s) -> {
            try {
                s.sendMessage(textMessage);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("handleTransportError:id={}", session.getId(), exception);
        sessionHolder.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("afterConnectionClosed:id={}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

```
### 拦截器 HandshakeInterceptor
编写 HandshakeInterceptor 进行拦截器处理
```java
package com.nekonekod.learnwebsocket.springws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * Author: duwenjun
 * Date: 2018/03/12
 * Time: 下午6:11
 * Project: learn-websocket
 *
 * 
 */
@Component
public class HandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        return super.beforeHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception ex) {
        super.afterHandshake(request, response, wsHandler, ex);
    }
}

```

### 编写 SpringWebSocketConfig 配置类
spring发布websocket服务可以使用xml注解，也可以使用实现 WebSocketConfigurer 接口声明配置，这里采用后者  
在registerWebSocketHandlers方法中调用registry.addHandler添加对应路径和相应的处理类、拦截器

```java
package com.nekonekod.learnwebsocket.springws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * Author: duwenjun
 * Date: 2018/03/12
 * Time: 下午7:33
 * Project: learn-websocket
 *
 * 
 */
@Configuration
@EnableWebMvc
@EnableWebSocket
public class SpringWebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {

    @Resource
    private SpringWebSocketHandler springWebSocketHandler;

    @Resource
    private HandshakeInterceptor interceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(springWebSocketHandler, "/springws")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}

```

## 使用netty
### 创建server类
编写WebSocketServer继承TimerTask，实现ApplicationListener<ContextRefreshedEvent>  
在spring项目启动后，异步创建netty server，代码如下  
```java
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
 *
 * 
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

```
其中childChannelHandler是IO处理类，即处理websocket消息处理类，实现类是WebSocketChildChannelHandler，在xml中配置注入  
WebSocketChildChannelHandler继承ChannelInitializer<SocketChannel>
```java
package com.nekonekod.learnwebsocket.nettyws;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Author: duwenjun
 * Date: 2018/03/13
 * Time: 上午10:44
 * Project: learn-websocket
 *
 * 
 */
@Component
public class WebSocketChildChannelHandler extends ChannelInitializer<SocketChannel> {

    @Resource(name = "webSocketServerHandler")
    private ChannelHandler webSocketServerHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast("http-codec", new HttpServerCodec());
        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
        ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
        ch.pipeline().addLast("handler", webSocketServerHandler);
    }
}
```
webSocketServerHandler是具体处理websocket的类，它提供的方法和以上两种方式类似，包括连接建立，消息处理，连接异常，连接中断等，不过是基于netty的。注意第一次是基于http的，其中包含了ws的一些信息，后面请求则是websocket
```java
package com.nekonekod.learnwebsocket.nettyws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * Author: duwenjun
 * Date: 2018/03/13
 * Time: 上午10:46
 * Project: learn-websocket
 *
 * 
 */
@Log4j2
@Component
@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
        push(ctx, "连接成功");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //http：//xxxx
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            //ws://xxxx
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    //异常处理，netty默认是关闭channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error(cause.getMessage(), cause);
        ctx.close();
    }



    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        //关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        //ping请求
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //只支持文本格式，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new RuntimeException("仅支持文本格式");
        }
        //客服端发送过来的消息
        String request = ((TextWebSocketFrame) frame).text();
        log.info("服务端收到：{}", request);

        push(channelGroup, "[netty] :" + request);
    }

    //第一次请求是http请求，请求头包括ws的信息
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws:/" + ctx.channel() + "/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            //不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }


    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }

    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        return false;
    }

    /**
     * 推送单个
     */
    public static void push(final ChannelHandlerContext ctx, final String message) {
        TextWebSocketFrame tws = new TextWebSocketFrame(message);
        ctx.channel().writeAndFlush(tws);

    }

    /**
     * 群发
     */
    public static void push(final ChannelGroup ctxGroup, final String message) {
        TextWebSocketFrame tws = new TextWebSocketFrame(message);
        ctxGroup.writeAndFlush(tws);

    }
}

```
webSocketServer中一些属性需要注入，xml配置文件如下
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd   http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <mvc:annotation-driven/>
    <context:component-scan base-package="com.nekonekod.learnwebsocket"/>


    <!-- 对静态资源文件的访问-->
    <mvc:resources mapping="/static/**" location="/static/" cache-period="31556926"/>

    <context:property-placeholder location="classpath*:app.properties"/>

    <bean id="bossGroup" class="io.netty.channel.nio.NioEventLoopGroup"/>
    <bean id="workerGroup" class="io.netty.channel.nio.NioEventLoopGroup"/>
    <bean id="serverBootstrap" class="io.netty.bootstrap.ServerBootstrap" scope="prototype"/>
    <bean id="webSocketServer" class="com.nekonekod.learnwebsocket.nettyws.WebSocketServer">
        <property name="port" value="${websocket.server.port}"/>
        <property name="childChannelHandler" ref="webSocketChildChannelHandler"/>
    </bean>
</beans>
```

前端页面，简易的类聊天室功能，通过单选框切换websocket方式
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title></title>
</head>
<body>
websocket Demo<br/>
<label>
    <input type="radio" name="handler" checked value="defaultws" onchange="changeHandler(this.value)"/>javax
</label>
<label>
    <input type="radio" name="handler" value="springws" onchange="changeHandler(this.value)"/>spring
</label>
<label>
    <input type="radio" name="handler" value="netty" onchange="changeHandler(this.value)"/>netty
</label>
<div id="message"></div>
<input id="text" type="text"/>
<button onclick="send()"> Send</button>
<button onclick="closeWebSocket()"> Close</button>
<script type="text/javascript">

    //判断当前浏览器是否支持WebSocket
    if ('WebSocket' in window) {
        changeHandler('defaultws')
        console.log("link success")
    } else {
        alert('Not support websocket')
    }

    var websocket;

    //将消息显示在网页上
    function setMessageInnerHTML(innerHTML) {
        document.getElementById('message').innerHTML += innerHTML + '<br/>';
    }

    //关闭连接
    function closeWebSocket() {
        websocket.close();
    }

    //发送消息
    function send() {
        var message = document.getElementById('text').value;
        websocket.send(message);
    }

    function changeHandler(handler) {
        if (websocket) closeWebSocket()
        if(handler === 'netty'){
            websocket = new WebSocket("ws://127.0.0.1:7397/");
        }else{
            websocket = new WebSocket("ws://127.0.0.1:8080/" + handler);
        }
        console.log("link success")


        //连接发生错误的回调方法
        websocket.onerror = function () {
            setMessageInnerHTML(handler + " error");
        };

        //连接成功建立的回调方法
        websocket.onopen = function (event) {
            setMessageInnerHTML(handler + " open");
        }
        console.log("-----")
        //接收到消息的回调方法
        websocket.onmessage = function (event) {
            setMessageInnerHTML(event.data);
        }

        //连接关闭的回调方法
        websocket.onclose = function () {
            setMessageInnerHTML(handler + " close");
        }

        //监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
        window.onbeforeunload = function () {
            websocket.close();
        }
    }
</script>

</body>
</html>
```