package com.dot618;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by xiaocj on 1/29/16.
 */
public class ChannelCodec extends ByteToMessageCodec<JSONObject> {
    private static final Logger log = LogManager.getLogger(ChannelCodec.class);

    private static final int MSG_HEADER_MAGIC = 0x7e7e7e7e;
    private static final int MSG_MAX_LEN = 128 * 1024;
    private static final int MSG_HEADER_LEN = 8;

    @Override
    protected void encode(ChannelHandlerContext ctx, JSONObject msg, ByteBuf out) throws Exception {

        String resp = msg.toString();
        log.info(String.format("send msg='%s", resp));

        final byte[] bytes = resp.getBytes();
        out.writeInt(MSG_HEADER_MAGIC);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        try {
            int len = prepareReadMsg(ctx, in);
            while(len >= 0) {
                final byte[] data = new byte[len];
                in.readBytes(data);

                String req = new String(data, "UTF-8");
                log.info(String.format("recv msg='%s", req));
                JSONObject json = new JSONObject(req);

                out.add(json);

                len = prepareReadMsg(ctx, in);
            }
        }
        catch (Exception e) {
            log.warn("Encounter exception, cause=%s", e.getCause());
            ctx.close();
        }
    }


    /**
     * if msg is ready, return the msg len, otherwise return -1
      */
    private int prepareReadMsg(ChannelHandlerContext ctx, ByteBuf in) {
        if(in.readableBytes() < MSG_HEADER_LEN) {
            return -1;
        }

        in.markReaderIndex();
        int header = in.readInt();
        int len = in.readInt();

        if(header != MSG_HEADER_MAGIC) {
            log.warn(String.format("Invalid header, abort. header=0x%x", header));
            ctx.close();
            return -1;
        }

        if(len > MSG_MAX_LEN || len < 0) {
            log.warn(String.format("Invalid message length, abort. len=%d", len));
            ctx.close();
            return -1;
        }

        if(in.readableBytes() < len) {
            in.resetReaderIndex();
            return -1;
        }

        return len;
    }
}
