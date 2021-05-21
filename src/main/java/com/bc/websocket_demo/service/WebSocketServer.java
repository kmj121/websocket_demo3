package com.bc.websocket_demo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@ServerEndpoint("/imserver/{userId}")
@Component
public class WebSocketServer {
    static Log log = LogFactory.getLog(WebSocketServer.class);

    /**静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。*/
    private static int onlineCount = 0;
    /**concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。*/
    private static ConcurrentHashMap<String, List<WebSocketServer>> webSocketMap = new ConcurrentHashMap<>();
    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;
    /**接收userId*/
    private String userId="";

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        this.session = session;
        this.userId=userId;
        List<WebSocketServer> list = new ArrayList<>();
        if(webSocketMap.containsKey(userId)){
//            webSocketMap.remove(userId);
            list = webSocketMap.get(userId);
            list.add(this);
            webSocketMap.put(userId,list);
        }else{
            list.add(this);
            webSocketMap.put(userId,list);
            //在线数加1
            addOnlineCount();
        }

        log.info("用户连接:"+userId+",当前在线人数为:" + getOnlineCount());

        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户:"+userId+",网络异常!!!!!!");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if(webSocketMap.containsKey(userId)){
            List<WebSocketServer> list = webSocketMap.get(userId);
            List<WebSocketServer> result = new ArrayList<>();
            for (WebSocketServer item : list) {
                if (item != this) {
                    result.add(item);
                }
            }
            if (result.size() == 0) {
                webSocketMap.remove(userId);
                subOnlineCount();
            } else {
                webSocketMap.put(userId, result);
            }
        }
        log.info("用户退出:"+userId+",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息:"+userId+",报文:"+message);
        //可以群发消息
        //消息保存到数据库、redis
        if(StringUtils.isNotBlank(message)){
            try {
                //解析发送的报文
                JSONObject jsonObject = JSON.parseObject(message);
                //追加发送人(防止串改)
                jsonObject.put("fromUserId",this.userId);
                String toUserId=jsonObject.getString("toUserId");
                //传送给对应toUserId用户的websocket
                if(StringUtils.isNotBlank(toUserId)&&webSocketMap.containsKey(toUserId)){
                    List<WebSocketServer> list = webSocketMap.get(toUserId);
                    for (WebSocketServer item : list) {
                        item.sendMessage(jsonObject.toJSONString());
                    }
                }else{
                    log.error("请求的userId:"+toUserId+"不在该服务器上");
                    //否则不在这个服务器上，发送到mysql或者redis
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:"+this.userId+",原因:"+error.getMessage());
        error.printStackTrace();
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 发送自定义消息
     * */
    public static void sendInfo(String message,@PathParam("userId") String userId) throws IOException {
        log.info("发送消息到:"+userId+"，报文:"+message);
        if(StringUtils.isNotBlank(userId)&&webSocketMap.containsKey(userId)){
            List<WebSocketServer> list = webSocketMap.get(userId);
            for (WebSocketServer item : list) {
                item.sendMessage(message);
            }
        }else{
            log.error("用户"+userId+",不在线！");
        }
    }

    public static void sendEveryOneInfo() throws IOException {
        log.info("发送不同消息给每一个人");
        if (webSocketMap.size() > 0) {
            for (String s : webSocketMap.keySet()) {
                if (webSocketMap.containsKey("10") && s.equals("10")) {
                    List<WebSocketServer> list = webSocketMap.get("10");
                    for (WebSocketServer item : list) {
                        item.sendMessage("hello 10");
                    }
                }
                if (webSocketMap.containsKey("20") && s.equals("20")) {
                    List<WebSocketServer> list = webSocketMap.get("20");
                    for (WebSocketServer item : list) {
                        item.sendMessage("hello 20");
                    }
                }
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebSocketServer that = (WebSocketServer) o;
        return Objects.equals(session, that.session) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session, userId);
    }
}
