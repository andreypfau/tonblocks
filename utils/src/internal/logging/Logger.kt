package io.tonblocks.utils.internal.logging

public interface LoggerFactory {
    public fun getLogger(name: String): Logger

    public fun getLogger(func: () -> Unit): Logger
}

interface Logger {
    public val level: LogLevel

    public fun log(level: LogLevel, msg: () -> Any?)

    public fun log(level: LogLevel, t: Throwable?, msg: () -> Any?)

    public fun debug(msg: () -> Any?) = log(LogLevel.DEBUG, msg)

    public fun debug(t: Throwable?, msg: () -> Any?) = log(LogLevel.DEBUG, t, msg)

    public fun error(msg: () -> Any?) = log(LogLevel.ERROR, msg)

    public fun error(t: Throwable?, msg: () -> Any?) = log(LogLevel.ERROR, t, msg)

    public fun info(msg: () -> Any?) = log(LogLevel.INFO, msg)

    public fun info(t: Throwable?, msg: () -> Any?) = log(LogLevel.INFO, t, msg)

    public fun trace(msg: () -> Any?) = log(LogLevel.TRACE, msg)

    public fun trace(t: Throwable?, msg: () -> Any?) = log(LogLevel.TRACE, t, msg)

    public fun warn(msg: () -> Any?) = log(LogLevel.WARN, msg)

    public fun warn(t: Throwable?, msg: () -> Any?) = log(LogLevel.WARN, t, msg)
}

public val Logger.isTraceEnabled: Boolean get() = level <= LogLevel.TRACE

/**
 * Logs an error from an [exception] using its message
 */
public fun Logger.error(exception: Throwable) {
    error(exception) {
        exception.message ?: "Exception of type ${exception::class}"
    }
}
