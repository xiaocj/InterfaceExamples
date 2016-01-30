package com.dot618;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.function.Consumer;

/**
 * Created by xiaocj on 1/29/16.
 */
public class MessageHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(MessageHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        JSONObject json = new JSONObject();
        json.put("cmd", "list");
        json.put("cmdType", "getDeviceList");
        json.put("gateWayId", "gateway");
        json.put("uid", "12345");
        ctx.writeAndFlush(json);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        JSONObject json = (JSONObject) msg;
        String cmdType = json.getString("cmdType");
        if(cmdType == null || !cmdType.equals("getDeviceList")) {
            return;
        }

        // process the device list
        try {
            JSONArray keyList = json.getJSONArray("groupList");
            if(keyList != null) {
                // show the LUME keys
                keyList.forEach(new Consumer<Object>() {
                    @Override
                    public void accept(Object obj) {
                        if(obj instanceof JSONObject) {
                            JSONObject jo = (JSONObject) obj;
                            log.info(String.format("Device(%d): name=%s, online=%s, onOff=%s",
                                    jo.getInt("id"), jo.getString("name"),
                                    jo.getString("connect").equals("online"),
                                    jo.getString("onOff").equals("on")));
                        }
                    }
                });

                // flip the first key
                if(keyList.length() > 0) {
                    JSONObject jo = keyList.getJSONObject(0);

                    JSONObject flipCmd = new JSONObject();
                    flipCmd.put("cmd", "device");
                    flipCmd.put("cmdType", "setOnOff");
                    flipCmd.put("id", jo.getInt("id"));
                    flipCmd.put("onOff", jo.getString("onOff").equals("on") ? "off" : "on");

                    ctx.writeAndFlush(flipCmd);
                }
            }
        } finally {
        }
    }
}
