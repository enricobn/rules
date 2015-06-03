package org.rules;

import scala.Option;
import scala.Predef;
import scala.Tuple2;
import scala.collection.JavaConverters;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

/**
 * Created by enrico on 5/21/15.
 */
public class JavaUtils {

    public static scala.collection.immutable.Map<String, Object> eval(ScriptEngine engine, String script) throws ScriptException {
        Map<String, Object> map = (Map<String, Object>) engine.eval(script);

        return JavaConverters.mapAsScalaMapConverter(map).asScala().toMap(
                Predef.<Tuple2<String, Object>>conforms()
        );
    }

    public static JavaUI toJavaUI(final UI ui) {
        return new JavaUI() {
            @Override
            public <T> T choose(String title, String message, List<T> items) {
                Option<T> result = ui.choose(title, message, JavaConverters.asScalaBufferConverter(items).asScala().toList());
                if (result.isDefined()) {
                    return result.get();
                } else {
                    return null;
                }
            }
        };
    }

}
