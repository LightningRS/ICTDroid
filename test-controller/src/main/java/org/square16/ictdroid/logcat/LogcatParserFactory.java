package org.square16.ictdroid.logcat;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.square16.ictdroid.logcat.annotations.LogcatParser;
import org.square16.ictdroid.logcat.interfaces.ILogcatParser;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Zsx
 */
@Slf4j
public class LogcatParserFactory {
    private static final Map<String, ILogcatParser> PARSER_CLASSES = new ConcurrentHashMap<>();
    private static volatile boolean IS_INIT = false;

    private static void loadParserClasses() {
        Reflections ref = new Reflections(LogcatParserFactory.class.getPackageName() + ".parser");
        Set<Class<? extends ILogcatParser>> classes = ref.getSubTypesOf(ILogcatParser.class);
        for (Class<? extends ILogcatParser> clazz : classes) {
            LogcatParser parserInfo = clazz.getAnnotation(LogcatParser.class);
            try {
                PARSER_CLASSES.put(parserInfo.verbosity(), clazz.getDeclaredConstructor().newInstance());
                log.debug("Registered logcat parser [{}] for verbosity [{}]", clazz.getName(), parserInfo.verbosity());
            } catch (ReflectiveOperationException e) {
                log.error("Failed to create parser for class [{}]", clazz.getName(), e);
            }
        }
    }

    public static ILogcatParser getParser(String verbosity) {
        if (!IS_INIT) {
            synchronized (LogcatParserFactory.class) {
                if (!IS_INIT) {
                    loadParserClasses();
                    IS_INIT = true;
                }
            }
        }
        if (!PARSER_CLASSES.containsKey(verbosity)) {
            log.error("No parser found for logcat verbosity: {}", verbosity);
            return null;
        }
        return PARSER_CLASSES.get(verbosity);
    }
}
