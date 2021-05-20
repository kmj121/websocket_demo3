package com.bc.websocket_demo.controller;

import com.bc.websocket_demo.entity.SocketMessage;
import com.bc.websocket_demo.entity.SocketResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    /**
     * 当浏览器向服务端发送请求时，通过@MessageMapping映射/welcome这个地址，类似于@RequestMapping
     * SendTo：当服务端有消息是，会对订阅了@SendTo中的路径的浏览器发送消息
     */
    @MessageMapping("/welcome")
    @SendTo("/topic/getResponse")
    public SocketResponse say(SocketMessage message) throws InterruptedException {
//        Thread.sleep(3000);
        return new SocketResponse("Welcome,"+message.getName()+"!");
    }
}