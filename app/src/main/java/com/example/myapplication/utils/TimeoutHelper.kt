package com.example.myapplication.utils

import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeoutHelper @Inject constructor() {

    companion object {
        // 不同操作的默认超时时间（毫秒）
        const val ROOT_REQUEST_TIMEOUT = 10_000L
        const val ROOT_CHECK_TIMEOUT = 5_000L
        const val ACCESSIBILITY_CHECK_TIMEOUT = 3_000L
        const val NETWORK_REQUEST_TIMEOUT = 15_000L
        const val FILE_OPERATION_TIMEOUT = 5_000L
        const val QUICK_OPERATION_TIMEOUT = 2_000L
    }

    /**
     * 执行带超时的操作
     */
    suspend fun <T> withTimeout(
        timeoutMs: Long,
        operation: suspend () -> T
    ): T {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                operation()
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Operation timed out after ${timeoutMs}ms")
            throw TimeoutException("Operation timed out after ${timeoutMs}ms")
        }
    }

    /**
     * 执行带超时的操作，超时时返回默认值
     */
    suspend fun <T> withTimeoutOrDefault(
        timeoutMs: Long,
        defaultValue: T,
        operation: suspend () -> T
    ): T {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                operation()
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Operation timed out after ${timeoutMs}ms, returning default value")
            defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Operation failed, returning default value")
            defaultValue
        }
    }

    /**
     * 执行带超时的操作，超时时返回null
     */
    suspend fun <T> withTimeoutOrNull(
        timeoutMs: Long,
        operation: suspend () -> T
    ): T? {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                operation()
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Operation timed out after ${timeoutMs}ms")
            null
        } catch (e: Exception) {
            Timber.e(e, "Operation failed")
            null
        }
    }

    /**
     * Root权限请求专用超时处理
     */
    suspend fun <T> withRootTimeout(
        operation: suspend () -> T
    ): T {
        return withTimeout(ROOT_REQUEST_TIMEOUT, operation)
    }

    /**
     * Root权限检查专用超时处理
     */
    suspend fun <T> withRootCheckTimeout(
        operation: suspend () -> T
    ): T {
        return withTimeout(ROOT_CHECK_TIMEOUT, operation)
    }

    /**
     * 无障碍服务检查专用超时处理
     */
    suspend fun <T> withAccessibilityTimeout(
        operation: suspend () -> T
    ): T {
        return withTimeout(ACCESSIBILITY_CHECK_TIMEOUT, operation)
    }

    /**
     * 网络请求专用超时处理
     */
    suspend fun <T> withNetworkTimeout(
        operation: suspend () -> T
    ): T {
        return withTimeout(NETWORK_REQUEST_TIMEOUT, operation)
    }

    /**
     * 快速操作专用超时处理
     */
    suspend fun <T> withQuickTimeout(
        operation: suspend () -> T
    ): T {
        return withTimeout(QUICK_OPERATION_TIMEOUT, operation)
    }

    /**
     * 重试机制：在指定次数内重试操作
     */
    suspend fun <T> retry(
        maxAttempts: Int = 3,
        delayMs: Long = 1000L,
        operation: suspend (attempt: Int) -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Attempt ${attempt + 1} failed, retrying...")
                
                if (attempt < maxAttempts - 1) {
                    delay(delayMs)
                }
            }
        }
        
        throw lastException ?: Exception("All retry attempts failed")
    }

    /**
     * 带超时的重试机制
     */
    suspend fun <T> retryWithTimeout(
        maxAttempts: Int = 3,
        timeoutMs: Long = ROOT_REQUEST_TIMEOUT,
        delayMs: Long = 1000L,
        operation: suspend (attempt: Int) -> T
    ): T {
        return withTimeout(timeoutMs) {
            retry(maxAttempts, delayMs, operation)
        }
    }

    /**
     * 并发执行多个操作，等待所有完成或超时
     */
    suspend fun <T> awaitAll(
        timeoutMs: Long,
        operations: List<suspend () -> T>
    ): List<T> {
        return withTimeout(timeoutMs) {
            coroutineScope {
                operations.map { operation ->
                    async { operation() }
                }.awaitAll()
            }
        }
    }

    /**
     * 并发执行多个操作，返回第一个完成的结果
     */
    suspend fun <T> awaitAny(
        timeoutMs: Long,
        operations: List<suspend () -> T>
    ): T {
        return withTimeout(timeoutMs) {
            coroutineScope {
                val deferreds = operations.map { operation ->
                    async { operation() }
                }
                // 等待第一个完成的任务
                deferreds.first().await()
            }
        }
    }

    /**
     * 创建一个可取消的操作
     */
    fun createCancellableOperation(
        scope: CoroutineScope,
        operation: suspend () -> Unit
    ): Job {
        return scope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                Timber.d("Operation was cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Operation failed")
                throw e
            }
        }
    }
}