package org.easyarch.netcat.test.handler;

import org.easyarch.netcat.http.protocol.HttpHeaderValue;
import org.easyarch.netcat.http.request.impl.HttpHandlerRequest;
import org.easyarch.netcat.http.response.impl.HttpHandlerResponse;
import org.easyarch.netcat.kits.file.FileKits;
import org.easyarch.netcat.mvc.action.handler.HttpHandler;

/**
 * Created by xingtianyu on 17-3-14
 * 下午2:41
 * description:
 */

public class DownLoadHandler implements HttpHandler {

    @Override
    public void handle(HttpHandlerRequest request, HttpHandlerResponse response) throws Exception {
        byte[] pdf = FileKits.read("/home/code4j/58daojia/技术/文档/mybatis-plus.pdf");
        response.download(pdf,"mybatis权威指南", HttpHeaderValue.PDF);
    }
}