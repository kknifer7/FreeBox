module io.knifer.freebox {
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
    requires jregistry;

    requires static lombok;

    exports io.knifer.freebox;
    exports io.knifer.freebox.controller;
    exports io.knifer.freebox.component.converter;
    exports io.knifer.freebox.component.event;
    exports io.knifer.freebox.model.domain;
    exports io.knifer.freebox.model.common;
    exports io.knifer.freebox.component.factory;
    opens io.knifer.freebox to javafx.fxml;
    opens io.knifer.freebox.controller to javafx.fxml;
    opens io.knifer.freebox.model.domain to com.google.gson;
    opens io.knifer.freebox.model.common to com.google.gson;
    opens io.knifer.freebox.model.c2s to com.google.gson;
    opens io.knifer.freebox.model.s2c to com.google.gson;
}