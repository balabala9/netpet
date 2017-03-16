package org.easyarch.netcat.http.request.impl;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.easyarch.netcat.context.HandlerContext;
import org.easyarch.netcat.http.cookie.HttpCookie;
import org.easyarch.netcat.http.request.HandlerRequest;
import org.easyarch.netcat.http.request.ParamParser;
import org.easyarch.netcat.http.session.HttpSession;
import org.easyarch.netcat.kits.StringKits;
import org.easyarch.netcat.mvc.router.Router;

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.*;

import static org.easyarch.netcat.http.Const.NETCATID;
import static org.easyarch.netcat.http.protocol.HttpHeaderName.COOKIE;

/**
 * Description :
 * Created by xingtianyu on 17-2-24
 * 下午5:19
 * description:
 */

public class HttpHandlerRequest implements HandlerRequest {

    private FullHttpRequest request;
    private HttpHeaders headers;
    private Channel channel;

    private Router router;

    private HandlerContext context;

    private String charset;
    private ParamParser paramParser;
    private Map<String,String> params ;
    private ServerCookieDecoder decoder;
    private static final String QUESTION = "?";

    public HttpHandlerRequest(FullHttpRequest request,Router router, HandlerContext context, Channel channel){
        this.request = request;
        this.context = context;
        this.router = router;
        this.channel = channel;
        checkRequest(request);
        this.decoder = ServerCookieDecoder.LAX;
        this.charset = "UTF-8";
        this.paramParser = new ParamParser(request);
        this.params = paramParser.parse();
        this.params.putAll(router.getPathParams());

    }

    private void checkRequest(FullHttpRequest request){
        if (request == null){
            this.headers = new DefaultHttpHeaders();
        }else{
            this.headers = request.headers();
        }
    }

    private Set<HttpCookie> wrapCookies(Set<Cookie> cookies){
        Set<HttpCookie> httpCookies = new HashSet<>();
        for (Cookie cookie:cookies){
            HttpCookie hc = new HttpCookie();
            hc.setMaxAge(cookie.maxAge());
            hc.setDomain(cookie.domain());
            hc.setHttpOnly(cookie.isHttpOnly());
            hc.setName(cookie.name());
            hc.setValue(cookie.value());
            hc.setPath(cookie.path());
            httpCookies.add(hc);
        }
        return httpCookies;
    }

    private String checkURL(String url){
        if (StringKits.isEmpty(url)){
            return null;
        }
        int index = url.lastIndexOf(QUESTION);
        if (index == -1){
            return null;
        }
        String queryString = url.substring(url.lastIndexOf(QUESTION) + 1,url.length());
        return encode(queryString);
    }

    public HandlerContext getContext() {
        return context;
    }

    public Set<HttpCookie> getCookies() {
        String cookieValue = this.headers.get(COOKIE);
        Set<Cookie> cookies = decoder.decode(cookieValue);
        return wrapCookies(cookies);
    }


    public String getHeader(String name) {
        if (this.headers == null){
            return "";
        }
        String header = headers.get(name);
        return encode(header);
    }

    public Collection<String> getHeaderNames() {
        if (this.headers == null){
            return new ArrayList<>();
        }
        return this.headers.names();
    }

   
    public Long getDateHeader(String name) {
        if (this.headers == null){
            return null;
        }
        return this.headers.getTimeMillis(name);
    }

   
    public String getMethod() {
        return request.method().name();
    }

    public String getContextPath() {
        return context.getContextPath();
    }

   
    public String getQueryString() {
        String url = request.uri();
        return checkURL(url);
    }

    private String encode(String content){
        if (StringKits.isEmpty(content)){
            return null;
        }
        try {
            return new String(content.getBytes(),charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return content;
        }
    }
    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    public String getRequestURI() {
        return router.getPath();
    }

    public HttpSession getSession() {
        String sessionId = "";
        for(HttpCookie cookie:getCookies()){
            if (NETCATID.equals(cookie.name())){
                sessionId = cookie.value();
            }
        }
        return context.getSession(sessionId);
    }


    public String getCharacterEncoding() {
        return this.charset;
    }

   
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.charset = Charset.forName(env).name();
    }

   
    public int getContentLength() {
        String length = getHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
        if (StringKits.isEmpty(length)) {
            return 0;
        }
        return Integer.parseInt(length);
    }


    public String getContentType() {
        String length = this.headers.get(HttpHeaderNames.CONTENT_TYPE);
        if (StringKits.isEmpty(length)){
            return null;
        }
        return length;
    }


    public String getParameter(String name) {
        String value = this.params.get(name);
        return encode(value);
    }

   
    public Collection<String> getParameterNames() {
        return this.params.keySet();
    }

   
    public Collection<String> getParameterValues(String name) {
        List<String> values = new ArrayList<>();
        for (String val:this.params.values()){
            values.add(encode(val));
        }
        return values;
    }

   
    public Map<String, String> getParameterMap() {
        return this.params;
    }

   
    public String getProtocol() {
        return request.protocolVersion().text();
    }

    public String getRemoteAddr() {
        return channel.remoteAddress().toString();
    }

   
}
