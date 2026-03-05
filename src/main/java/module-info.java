module io.knifer.freebox {
    requires java.desktop;
    requires jdk.httpserver;

    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires com.google.gson;
    requires com.google.common;
    requires jsr305;
    requires org.apache.commons.lang3;
    requires org.java_websocket;
    requires blockingMap4j;
    requires uk.co.caprica.vlcj;
    requires uk.co.caprica.vlcj.javafx;
    requires uk.co.caprica.vlcj.natives;
    requires org.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.impl;
    requires filelize;
    requires com.fasterxml.jackson.databind;
    requires okhttp3;
    requires annotations;
    requires cn.hutool.core;
    requires cn.hutool.crypto;
    requires cn.hutool.http;
    requires cn.hutool.json;
    requires cn.hutool.system;
    requires net.bjoernpetersen.m3u;
    requires mpv;
    requires ipcsocket;
    requires emojiJava;
    requires org.graalvm.polyglot;
    requires org.graalvm.js;
    requires com.google.guice;
    requires jakarta.inject;

    // for spiders
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires org.json;
    requires org.jsoup;

    requires static lombok;

    uses org.tinylog.slf4j.TinylogSlf4jServiceProvider;

    exports io.knifer.freebox;
    exports io.knifer.freebox.constant;
    exports io.knifer.freebox.controller;
    exports io.knifer.freebox.controller.dialog;
    exports io.knifer.freebox.component.converter;
    exports io.knifer.freebox.component.event;
    exports io.knifer.freebox.model.domain;
    exports io.knifer.freebox.model.common.tvbox;
    exports io.knifer.freebox.model.common.catvod;
    exports io.knifer.freebox.model.c2s;
    exports io.knifer.freebox.model.s2c;
    exports io.knifer.freebox.model.bo;
    exports io.knifer.freebox.spider;
    exports io.knifer.freebox.component.factory;
    exports io.knifer.freebox.net.websocket.core;
    exports io.knifer.freebox.component.router;
    exports io.knifer.freebox.context;
    exports io.knifer.freebox.spider.template;
    exports io.knifer.freebox.net;
    exports io.knifer.freebox.handler.impl;
    exports io.knifer.freebox.model.common.diyp;

    opens io.knifer.freebox to javafx.fxml;
    opens io.knifer.freebox.controller to javafx.fxml;
    opens io.knifer.freebox.controller.dialog to javafx.fxml;
    opens io.knifer.freebox.component.node to javafx.fxml;
    opens io.knifer.freebox.model.domain to com.google.gson, com.fasterxml.jackson.databind, filelize;
    opens io.knifer.freebox.model.common.tvbox to com.google.gson;
    opens io.knifer.freebox.model.common.catvod to com.google.gson;
    opens io.knifer.freebox.model.common.diyp to com.google.gson;
    opens io.knifer.freebox.model.s2c to com.google.gson;
    opens io.knifer.freebox.spider to com.google.gson;
    opens io.knifer.freebox.model.c2s to com.fasterxml.jackson.databind, com.google.gson, filelize;
    opens com.fongmi.quickjs.bean to com.google.gson;
    // for google guice
    opens io.knifer.freebox.net.http.handler;
    opens io.knifer.freebox.net.http.server;
    opens io.knifer.freebox.net.websocket.server;
    opens io.knifer.freebox.net.websocket.converter;
    opens io.knifer.freebox.spider.template.impl;
    opens io.knifer.freebox.handler;
    opens io.knifer.freebox.handler.impl;
    opens io.knifer.freebox.component.validator;

    opens io.knifer.freebox.log.provider to org.tinylog.api;
    opens io.knifer.freebox.log.writer to org.tinylog.api;
    opens io.knifer.freebox.log.configuration to org.tinylog.api;
}