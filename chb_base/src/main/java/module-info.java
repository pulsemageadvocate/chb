module pulad.chb.base {
	requires transitive slf4j.api;
	requires transitive javafx.graphics;
	requires com.fasterxml.jackson.databind;
	exports pulad.chb.config;
    exports pulad.chb.constant;
    exports pulad.chb.dto;
    exports pulad.chb.interfaces;
    exports pulad.chb.util;
}