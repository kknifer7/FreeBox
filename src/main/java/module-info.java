module io.knifer.freebox {
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
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
    requires org.slf4j.simple;
    requires filelize;
    requires com.fasterxml.jackson.databind;
    requires okhttp3;
    requires kotlin.stdlib;
    requires annotations;
    requires org.json;
    requires cn.hutool;
    requires net.bjoernpetersen.m3u;
    requires mpv;
    requires ipcsocket;
    requires emojiJava;

    requires static lombok;
    requires org.jsoup;

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
    opens io.knifer.freebox to javafx.fxml;
    opens io.knifer.freebox.controller to javafx.fxml;
    opens io.knifer.freebox.controller.dialog to javafx.fxml;
    opens io.knifer.freebox.model.domain to com.google.gson, com.fasterxml.jackson.databind, filelize;
    opens io.knifer.freebox.model.common.tvbox to com.google.gson;
    opens io.knifer.freebox.model.common.catvod to com.google.gson;
    opens io.knifer.freebox.model.common.diyp to com.google.gson;
    opens io.knifer.freebox.model.c2s to com.fasterxml.jackson.databind, com.google.gson, filelize;
    opens io.knifer.freebox.model.s2c to com.google.gson;
    opens io.knifer.freebox.spider to com.google.gson;
}