package io.knifer.freebox.net.http.handler;

import com.sun.net.httpserver.HttpExchange;

public interface HttpHandler {

    boolean support(HttpExchange httpExchange);

    void handle(HttpExchange httpExchange);

}