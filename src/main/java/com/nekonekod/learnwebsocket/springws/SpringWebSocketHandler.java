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
