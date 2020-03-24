module pulad.chb {
	requires transitive javafx.base;
    requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.web;
	requires thymeleaf;
	requires java.xml;
	requires transitive rhino;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
	requires java.desktop;
	requires unbescape;
	requires transitive slf4j.api;
	requires jdk.xml.dom;
    opens templates;
    exports pulad.chb;
    exports pulad.chb.bbs;
    exports pulad.chb.dto;
    exports pulad.chb.constant;
    exports pulad.chb.util;
    exports pulad.chb.favorite to pulad.chb;
    exports pulad.chb.read.board;
    exports pulad.chb.read.thread;
}