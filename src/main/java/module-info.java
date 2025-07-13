module ftpclient {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;

    exports client.gui;
    exports client.gui.controllers;
    exports client.gui.models;
    exports client.gui.utils;
}