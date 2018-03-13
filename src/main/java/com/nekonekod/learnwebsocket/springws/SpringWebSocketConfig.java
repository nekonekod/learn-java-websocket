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
