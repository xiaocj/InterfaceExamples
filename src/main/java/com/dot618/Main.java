package com.dot618;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;


/**
 * Created by xiaocj on 1/29/16.
 */
public class Main {
    private static final int GATEWAY_PORT = 1153;

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        // config log4j
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();

        // get arguments
        if(args.length < 1) {
            log.error("Please input the IP address of gateway!");
            return;
        }
        String gatewayAddress = args[0];

        // start netty client
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bs = new Bootstrap();
            bs.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.pipeline().addLast(
                                    new ChannelCodec(),
                                    new MessageHandler()
                            );
                        }
                    });

            log.info(String.format("Connect to gateway, dest=%s:%d", gatewayAddress, GATEWAY_PORT));
            ChannelFuture f = bs.connect(gatewayAddress, GATEWAY_PORT).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.warn("Encounter exception, msg='%s'", e.getCause());
            workerGroup.shutdownGracefully();
        }
    }
}
