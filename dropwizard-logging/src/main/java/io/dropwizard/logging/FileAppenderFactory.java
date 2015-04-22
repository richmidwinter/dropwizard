package io.dropwizard.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.util.Size;
import io.dropwizard.validation.ValidationMethod;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.TimeZone;

/**
 * An {@link AppenderFactory} implementation which provides an appender that writes events to a file, archiving older
 * files as it goes.
 * <p/>
 * <b>Configuration Parameters:</b>
 * <table>
 *     <tr>
 *         <td>Name</td>
 *         <td>Default</td>
 *         <td>Description</td>
 *     </tr>
 *     <tr>
 *         <td>{@code type}</td>
 *         <td><b>REQUIRED</b></td>
 *         <td>The appender type. Must be {@code file}.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code threshold}</td>
 *         <td>{@code ALL}</td>
 *         <td>The lowest level of events to write to the file.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code currentLogFilename}</td>
 *         <td><b>REQUIRED</b></td>
 *         <td>The filename where current events are logged.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code archive}</td>
 *         <td>{@code true}</td>
 *         <td>Whether or not to archive old events in separate files.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code archivedLogFilenamePattern}</td>
 *         <td><b>REQUIRED</b> if {@code archive} is {@code true}.</td>
 *         <td>
 *             The filename pattern for archived files. {@code %d} is replaced with the date in {@code yyyy-MM-dd} form,
 *             and the fact that it ends with {@code .gz} indicates the file will be gzipped as it's archived. Likewise,
 *             filename patterns which end in {@code .zip} will be filled as they are archived.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code archivedFileCount}</td>
 *         <td>{@code 5}</td>
 *         <td>
 *             The number of archived files to keep. Must be greater than {@code 0}.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code maxFileSize}</td>
 *         <td>(unlimited)</td>
 *         <td>
 *             The maximum size of the currently active file before a rollover is triggered. The value can be expressed
 *             in bytes, kilobytes, megabytes, gigabytes, and terabytesyp by appending B, K, MB, GB, or TB to the
 *             numeric value.  Examples include 100MB, 1GB, 1TB.  Sizes can also be spelled out, such as 100 megabytes,
 *             1 gigabyte, 1 terabyte.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>{@code timeZone}</td>
 *         <td>{@code UTC}</td>
 *         <td>The time zone to which event timestamps will be converted.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code logFormat}</td>
 *         <td>the default format</td>
 *         <td>
 *             The Logback pattern with which events will be formatted. See
 *             <a href="http://logback.qos.ch/manual/layouts.html#conversionWord">the Logback documentation</a>
 *             for details.
 *         </td>
 *     </tr>
 * </table>
 *
 * @see AbstractAppenderFactory
 */
@JsonTypeName("file")
public class FileAppenderFactory extends AbstractAppenderFactory {
    @NotNull
    private String currentLogFilename;

    private boolean archive = true;

    private String archivedLogFilenamePattern;

    @Min(1)
    private int archivedFileCount = 5;

    private Size maxFileSize;

    @NotNull
    private TimeZone timeZone = TimeZone.getTimeZone("UTC");

    @JsonProperty
    public String getCurrentLogFilename() {
        return currentLogFilename;
    }

    @JsonProperty
    public void setCurrentLogFilename(String currentLogFilename) {
        this.currentLogFilename = currentLogFilename;
    }

    @JsonProperty
    public boolean isArchive() {
        return archive;
    }

    @JsonProperty
    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    @JsonProperty
    public String getArchivedLogFilenamePattern() {
        return archivedLogFilenamePattern;
    }

    @JsonProperty
    public void setArchivedLogFilenamePattern(String archivedLogFilenamePattern) {
        this.archivedLogFilenamePattern = archivedLogFilenamePattern;
    }

    @JsonProperty
    public int getArchivedFileCount() {
        return archivedFileCount;
    }

    @JsonProperty
    public void setArchivedFileCount(int archivedFileCount) {
        this.archivedFileCount = archivedFileCount;
    }

    @JsonProperty
    public Size getMaxFileSize() {
        return maxFileSize;
    }

    @JsonProperty
    public void setMaxFileSize(Size maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    @JsonProperty
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @JsonProperty
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @JsonIgnore
    @ValidationMethod(message = "must have archivedLogFilenamePattern if archive is true")
    public boolean isValidArchiveConfiguration() {
        return !archive || (archivedLogFilenamePattern != null);
    }

    @JsonIgnore
    @ValidationMethod(message = "when specifying maxFileSize, archivedLogFilenamePattern must contain %i")
    public boolean isValidForMaxFileSizeSetting() {
        return maxFileSize == null || (archive && archivedLogFilenamePattern.contains("%i"));
    }

    @Override
    public Appender<ILoggingEvent> build(LoggerContext context, String applicationName, Layout<ILoggingEvent> layout) {
        final FileAppender<ILoggingEvent> appender = buildAppender(context);
        appender.setName("file-appender");

        appender.setAppend(true);
        appender.setContext(context);

        LayoutWrappingEncoder<ILoggingEvent> layoutEncoder = new LayoutWrappingEncoder<>();
        layoutEncoder.setLayout(layout == null ? buildLayout(context, timeZone) : layout);
        appender.setEncoder(layoutEncoder);

        appender.setPrudent(false);
        addThresholdFilter(appender, threshold);
        appender.stop();
        appender.start();

        return wrapAsync(appender);
    }

    protected FileAppender<ILoggingEvent> buildAppender(LoggerContext context) {
        if (archive) {
            final RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setFile(currentLogFilename);

            final TimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> triggeringPolicy;
            if (maxFileSize == null) {
                triggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<>();
            } else {
                SizeAndTimeBasedFNATP<ILoggingEvent> maxFileSizeTriggeringPolicy = new SizeAndTimeBasedFNATP<>();
                maxFileSizeTriggeringPolicy.setMaxFileSize(String.valueOf(maxFileSize.toBytes()));
                triggeringPolicy = maxFileSizeTriggeringPolicy;
            }
            triggeringPolicy.setContext(context);

            final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setFileNamePattern(archivedLogFilenamePattern);
            rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(
                    triggeringPolicy);
            triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
            rollingPolicy.setMaxHistory(archivedFileCount);

            appender.setRollingPolicy(rollingPolicy);
            appender.setTriggeringPolicy(triggeringPolicy);

            rollingPolicy.setParent(appender);
            rollingPolicy.start();
            return appender;
        }

        final FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setFile(currentLogFilename);
        return appender;
    }
}
