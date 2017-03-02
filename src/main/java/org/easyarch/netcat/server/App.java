package org.easyarch.netcat.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.easyarch.netcat.context.HandlerContext;
import org.easyarch.netcat.kits.StringKits;
import org.easyarch.netcat.mvc.handler.HttpHandler;
import org.easyarch.netcat.server.handler.BaseChildHandler;

/**
 * Description :
 * Created by xingtianyu on 17-2-23
 * 下午4:22
 * description:
 */

public class App {

    private HandlerContext context;

    public App(){
        context = new HandlerContext();
    }

    public void start(int port) {
        launch(port);
    }
    private void launch(int port) {
        System.out.println("正在启动服务。。。,服务端口:" + port);
        EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 8);
        EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 8);
        ServerBootstrap b = new ServerBootstrap();
        ChannelFuture f = null;
        try {
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new BaseChildHandler())
                    .option(ChannelOption.SO_BACKLOG, 2048)
                    .option(ChannelOption.TCP_NODELAY,true);
            f = b.bind(port).sync();
            System.out.println("服务已启动");
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public App get(String url, HttpHandler httpHandler){
        if (StringKits.isEmpty(url)||httpHandler != null){
            return this;
        }
        context.addHandler(url,httpHandler);
        return this;
    }
}