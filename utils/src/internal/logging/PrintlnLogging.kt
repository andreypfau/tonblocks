package io.tonblocks.utils.internal.logging

class PrintlnLogger(
    val name: String,
    override val level: LogLevel = LogLevel.INFO
) : Logger {
    override fun log(level: LogLevel, msg: () -> Any?) {
        if (level < this.level) return
        println("[${level.name}] ($name): ${msg()}")
    }

    override fun log(level: LogLevel, t: Throwable?, msg: () -> Any?) {
        if (level < this.level) return
        println("[${level.name} ($name): ${msg()}. Cause: ${t?.stackTraceToString()}")
    }
}
