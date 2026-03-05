package io.knifer.freebox.log.writer;

import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.Writer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 参考：<a href="https://github.com/tinylog-org/tinylog-reload-configuration-example/">tinylog-reload-configuration-example</a>
 */
public class BlackHoleWriter implements Writer {

    public BlackHoleWriter(Map<String, String> properties) {}

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        return Collections.emptySet();
    }

    @Override
    public void write(LogEntry logEntry) {}

    @Override
    public void flush() {}

    @Override
    public void close() {}

}