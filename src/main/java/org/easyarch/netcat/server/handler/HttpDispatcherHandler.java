package org.easyarch.netcat.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.easyarch.netcat.context.ActionHolder;
import org.easyarch.netcat.context.HandlerContext;
import org.easyarch.netcat.http.protocol.*;
import org.easyarch.netcat.http.protocol.HttpMethod;
import org.easyarch.netcat.http.request.impl.HttpHandlerRequest;
import org.easyarch.netcat.http.response.impl.HttpHandlerResponse;
import org.easyarch.netcat.mvc.action.Action;
import org.easyarch.netcat.mvc.action.ActionWrapper;
import org.easyarch.netcat.mvc.action.filter.Filter;
import org.easyarch.netcat.mvc.action.handler.HttpHandler;
import org.easyarch.netcat.mvc.action.handler.impl.DefaultHttpHandler;
import org.easyarch.netcat.mvc.action.handler.impl.NotFoundHandler;
import org.easyarch.netcat.mvc.action.handler.impl.NotSupportHandler;
import org.easyarch.netcat.mvc.router.Router;

import java.util.List;


/**
 * Description :
 * Created by xingtianyu on 17-2-23
 * 下午4:52
 * description:
 */

public class HttpDispatcherHandler extends ChannelInboundHandlerAdapter {

    private static final String NOTFOUND_MSG = "<h1 align='center'>404 Not Found</h1>";

    private static final String REDIRECT = "redirect:";

    private HandlerContext context;
    private ActionHolder holder;

    private DefaultHttpHandler defaultHttpHandler;
    private NotSupportHandler notSupportHandler;
    private HttpHandler notFoundHttpHandler;
    public HttpDispatcherHandler(HandlerContext context, ActionHolder holder) {
        this.context = context;
        this.holder = holder;
        this.defaultHttpHandler = new DefaultHttpHandler();
        this.notFoundHttpHandler = new NotFoundHandler();
        this.notSupportHandler = new NotSupportHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        boolean isSuccess = request.decoderResult().isSuccess();
        if (!isSuccess) {
            ctx.close();
            return;
        }
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        Router router = new Router(request.uri(), HttpMethod.getMethod(request.method()));
        List<Filter> filters = holder.getFilters(router);
        ActionWrapper wrapper = holder.getAction(router);
        HttpHandlerRequest req = new HttpHandlerRequest(request,router,context,ctx.channel());
        HttpHandlerResponse resp = new HttpHandlerResponse(response,context,ctx.channel());
        defaultHttpHandler.handle(req,resp);
        if (defaultHttpHandler.isInterrupt()){
            return;
        }
        Action action = null;
        if (wrapper != null&&wrapper.getAction() != null){
            action = wrapper.getAction();
        }
        if (filters == null || filters.isEmpty() && action == null) {
            notFoundHttpHandler.handle(req,resp);
            return;
        }
        if (wrapper.getStatus() == HttpStatus.METHOD_NOT_ALLOWED){
            notSupportHandler.handle(req,resp);
            return ;
        }
        if (!filters.isEmpty()) {
            for (Filter filter : filters) {
                if (!filter.before(req, resp)){
                    return;
                }
            }
        }
        HttpHandler handler = (HttpHandler)action;
        handler.handle(req,resp);
        if (!filters.isEmpty()) {
            for (Filter filter : filters) {
                filter.after(req, resp);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        StringBuffer buffer = new StringBuffer();
        buffer.append("<h1>500 Server Error</h1>");
        StringBuffer errorBuffer = new StringBuffer();
        errorBuffer.append(cause.toString() );
        StackTraceElement[] elements = cause.getStackTrace();
        for (StackTraceElement e: elements){
            errorBuffer.append("<br/><span style='margin-left:50px'>at  ").append(e).append("</span>");
        }
        buffer.append("<p>" + errorBuffer.toString() + "</p>");
        byte[] content = buffer.toString().getBytes();
        ByteBuf buf = Unpooled.wrappedBuffer(content, 0, content.length);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        ctx.writeAndFlush(response);
    }
}
