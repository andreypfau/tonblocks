package io.tonblocks.utils.internal.logging

import kotlin.reflect.KClass

public interface CommonLogger : Logger {
    companion object {
        private val factory by lazy {
            object : LoggerFactory {
                override fun getLogger(name: String): Logger {
                    return PrintlnLogger(name)
                }

                override fun getLogger(func: () -> Unit): Logger {
                    return getLogger(func::class.qualifiedName ?: func::class.toString())
                }
            }
        }

        public fun logger(name: String): Logger {
            return factory.getLogger(name)
        }

        public fun <T : Any> logger(kClass: KClass<T>): Logger {
            return logger(kClass::class.simpleName ?: kClass.toString())
        }

        public inline fun <reified T : Any> logger(): Logger {
            return logger(T::class)
        }

        public fun logger(func: () -> Unit = {}): Logger {
            return factory.getLogger(func)
        }
    }
}
