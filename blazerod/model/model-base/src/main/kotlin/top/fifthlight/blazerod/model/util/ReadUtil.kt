package top.fifthlight.blazerod.model.util

import java.io.IOException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun ReadableByteChannel.readAll(buffer: ByteBuffer): Int {
    var length = 0
    while (buffer.hasRemaining()) {
        when (val len = read(buffer)) {
            -1 -> if (buffer.hasRemaining()) {
                throw IOException("Channel reached EOF")
            } else {
                break
            }

            0 -> throw IllegalStateException("Buffer is not blocking")
            else -> {
                length += len
            }
        }
    }
    return length
}

fun ReadableByteChannel.readRemaining(buffer: ByteBuffer): Int {
    var length = 0
    while (buffer.hasRemaining()) {
        when (val len = read(buffer)) {
            -1 -> break

            0 -> throw IllegalStateException("Buffer is not blocking")
            else -> {
                length += len
            }
        }
    }
    return length
}

fun FileChannel.readToBuffer(offset: Long, length: Long, readSizeLimit: Int): ByteBuffer {
    // First, let's try mapping into memory
    try {
        return map(FileChannel.MapMode.READ_ONLY, offset, length)
    } catch (_: IOException) {
    } catch (_: UnsupportedOperationException) { // For ZipFileSystem
    }

    // If mapping failed, just read it to memory
    if (length > readSizeLimit) {
        throw IOException("Data size exceeds limit: maximum is $readSizeLimit, but got $length")
    }
    // length can't be longer than readSizeLimit (which is Int), so it is safe to convert length to Int
    val chunkLength = length.toInt()
    val prevPosition = position()
    try {
        val binaryBuffer = ByteBuffer.allocateDirect(chunkLength)
        position(offset)
        readAll(binaryBuffer)
        binaryBuffer.flip()
        return binaryBuffer
    } finally {
        position(prevPosition)
    }
}

inline fun ByteBuffer.withRelativeLimit(relativeLimit: Int, crossinline func: () -> Unit) {
    val oldLimit = limit()
    val newLimit = position() + relativeLimit
    if (newLimit > oldLimit) {
        throw BufferOverflowException()
    }
    limit(newLimit)
    try {
        func()
    } finally {
        limit(oldLimit)
    }
}

fun ByteBuffer.getUByteNormalized() = get().toFloat() / 255f
fun ByteBuffer.getSByteNormalized() = (get().toFloat() / 127f).coerceAtLeast(-1f)
fun ByteBuffer.getUShortNormalized() = getShort().toFloat() / 65535f
fun ByteBuffer.getSShortNormalized() = (getShort().toFloat() / 32767f).coerceAtLeast(-1f)

@Suppress("FloatingPointLiteralPrecision")
fun ByteBuffer.getUIntNormalized() = (getInt().toFloat() / 4294967295f).coerceAtLeast(-1f)

fun Float.toNormalizedUByte(): Byte = (this * 255.0f).roundToInt().toByte()
fun Float.toNormalizedSByte(): Byte = (this * 127.0f).roundToInt().toByte()
fun Float.toNormalizedUShort(): Short = (this * 65535.0f).roundToInt().toShort()
fun Float.toNormalizedSShort(): Short = (this * 32767.0f).roundToInt().toShort()
fun Float.toNormalizedUInt(): Int = (this * 4294967295.0).roundToLong().toInt()