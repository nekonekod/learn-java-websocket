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
