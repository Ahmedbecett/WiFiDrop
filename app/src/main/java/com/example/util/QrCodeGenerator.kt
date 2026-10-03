package com.example.util

import kotlin.math.min

/**
 * Self-contained pure-Kotlin QR Code Generator (ISO/IEC 18004 compatible).
 * Produces a 2D boolean grid (true = dark module, false = light module)
 * suitable for rendering on Compose Canvas or exporting to Bitmap.
 */
object QrCodeGenerator {

    enum class ErrorCorrectionLevel(val ordinalBits: Int, val formatBits: Int) {
        L(1, 0b01),
        M(0, 0b00),
        Q(3, 0b11),
        H(2, 0b10)
    }

    class QrMatrix(val size: Int, val modules: Array<BooleanArray>) {
        operator fun get(row: Int, col: Int): Boolean = modules[row][col]
    }

    // Reed-Solomon Galois Field GF(256) tables with primitive polynomial 0x11D (285)
    private val expTable = IntArray(256)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            logTable[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            // allows easy multiplication without modulo 255 on sum
        }
        logTable[0] = 0
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[(logTable[x] + logTable[y]) % 255]
    }

    // Generator polynomials for error correction
    private fun rsComputeEcc(data: ByteArray, eccCount: Int): ByteArray {
        var poly = intArrayOf(1)
        for (i in 0 until eccCount) {
            val root = expTable[i]
            val newPoly = IntArray(poly.size + 1)
            for (j in poly.indices) {
                newPoly[j] = newPoly[j] xor gfMul(poly[j], root)
                newPoly[j + 1] = newPoly[j + 1] xor poly[j]
            }
            poly = newPoly
        }

        val remainder = IntArray(eccCount)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor remainder[0]
            for (i in 0 until eccCount - 1) {
                remainder[i] = remainder[i + 1] xor gfMul(poly[poly.size - 1 - (i + 1)], factor)
            }
            remainder[eccCount - 1] = gfMul(poly[0], factor)
        }

        val result = ByteArray(eccCount)
        for (i in 0 until eccCount) {
            result[i] = remainder[i].toByte()
        }
        return result
    }

    // Capacity table for Version 1 to 10 (Total data codewords, ECC codewords per block for Level M)
    // For local Wi-Fi URLs like "http://192.168.1.100:8080" (26-45 chars), Version 3 (29x29) or Version 4 (33x33) is ideal!
    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val eccPerBlock: Int,
        val numBlocks: Int,
        val alignmentPatterns: IntArray
    ) {
        val size: Int get() = 17 + 4 * version
        val dataCodewords: Int get() = totalCodewords - (eccPerBlock * numBlocks)
    }

    private val versionSpecs = listOf(
        VersionSpec(1, 26, 10, 1, intArrayOf()),
        VersionSpec(2, 44, 16, 1, intArrayOf(6, 18)),
        VersionSpec(3, 70, 26, 1, intArrayOf(6, 22)),
        VersionSpec(4, 100, 18, 2, intArrayOf(6, 26)),
        VersionSpec(5, 134, 24, 2, intArrayOf(6, 30)),
        VersionSpec(6, 172, 16, 4, intArrayOf(6, 34))
    )

    fun encode(text: String): QrMatrix {
        val textBytes = text.toByteArray(Charsets.ISO_8859_1)

        // Find appropriate QR version
        var spec = versionSpecs.firstOrNull { it.dataCodewords - 3 >= textBytes.size }
            ?: versionSpecs.last()

        val dataBuffer = mutableListOf<Int>()
        // Mode indicator: 0100 for Byte mode (4 bits)
        val bitBuffer = BitBuffer()
        bitBuffer.append(0b0100, 4)
        // Character count indicator (8 bits for versions 1-9)
        bitBuffer.append(textBytes.size, 8)
        // Data payload
        for (b in textBytes) {
            bitBuffer.append(b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zeroes)
        val terminatorBits = min(4, spec.dataCodewords * 8 - bitBuffer.length)
        if (terminatorBits > 0) bitBuffer.append(0, terminatorBits)

        // Byte align with zeroes
        while (bitBuffer.length % 8 != 0) {
            bitBuffer.append(0, 1)
        }

        // Pad with alternating 0xEC and 0x11
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.length < spec.dataCodewords * 8) {
            bitBuffer.append(padBytes[padIndex], 8)
            padIndex = (padIndex + 1) % 2
        }

        val rawData = bitBuffer.toByteArray()
        // Error correction blocks
        val blockSize = rawData.size / spec.numBlocks
        val blocks = ArrayList<ByteArray>()
        val eccBlocks = ArrayList<ByteArray>()

        for (i in 0 until spec.numBlocks) {
            val start = i * blockSize
            val length = if (i == spec.numBlocks - 1) rawData.size - start else blockSize
            val blockData = rawData.copyOfRange(start, start + length)
            blocks.add(blockData)
            eccBlocks.add(rsComputeEcc(blockData, spec.eccPerBlock))
        }

        // Interleave data codewords
        val finalCodewords = ByteArray(spec.totalCodewords)
        var idx = 0
        val maxDataLen = blocks.maxOf { it.size }
        for (d in 0 until maxDataLen) {
            for (b in blocks) {
                if (d < b.size) finalCodewords[idx++] = b[d]
            }
        }
        for (e in 0 until spec.eccPerBlock) {
            for (ecc in eccBlocks) {
                if (e < ecc.size) finalCodewords[idx++] = ecc[e]
            }
        }

        // Build QR Matrix
        val size = spec.size
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        // Finder patterns (3 corners)
        fun placeFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val row = top + r
                    val col = left + c
                    if (row in 0 until size && col in 0 until size) {
                        val isBlack = (r in 0..6 && (c == 0 || c == 6)) ||
                                (c in 0..6 && (r == 0 || r == 6)) ||
                                (r in 2..4 && c in 2..4)
                        modules[row][col] = isBlack
                        isFunction[row][col] = true
                    }
                }
            }
        }
        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // Alignment patterns
        val alignCoords = spec.alignmentPatterns
        for (r in alignCoords) {
            for (c in alignCoords) {
                if (isFunction[r][c]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isBlack = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
                        modules[r + dr][c + dc] = isBlack
                        isFunction[r + dr][c + dc] = true
                    }
                }
            }
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            if (!isFunction[6][i]) {
                modules[6][i] = bit
                isFunction[6][i] = true
            }
            if (!isFunction[i][6]) {
                modules[i][6] = bit
                isFunction[i][6] = true
            }
        }

        // Dark module
        modules[4 * spec.version + 9][8] = true
        isFunction[4 * spec.version + 9][8] = true

        // Format info area reservation
        for (i in 0..8) {
            if (!isFunction[8][i]) isFunction[8][i] = true
            if (!isFunction[i][8]) isFunction[i][8] = true
        }
        for (i in size - 8 until size) {
            if (!isFunction[8][i]) isFunction[8][i] = true
            if (!isFunction[i][8]) isFunction[i][8] = true
        }

        // Place Data Codewords (zigzag traversal)
        var bitIndex = 0
        val totalBits = finalCodewords.size * 8
        var right = size - 1
        var upward = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing pattern
            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (r in rows) {
                for (col in intArrayOf(right, right - 1)) {
                    if (!isFunction[r][col]) {
                        var bit = false
                        if (bitIndex < totalBits) {
                            val byteVal = finalCodewords[bitIndex / 8].toInt() and 0xFF
                            val bitOffset = 7 - (bitIndex % 8)
                            bit = (byteVal shr bitOffset and 1) == 1
                            bitIndex++
                        }
                        // Apply Mask Pattern 0: (row + col) % 2 == 0
                        if ((r + col) % 2 == 0) {
                            bit = !bit
                        }
                        modules[r][col] = bit
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // Format info (Mask 0, ECC Level M -> 15 bits: 0b101010000010010 xor 0x5412 = 0x7CA8)
        val formatInfo = 0x7CA8
        for (i in 0..14) {
            val bit = (formatInfo shr (14 - i) and 1) == 1
            // Vertical placement
            val r = if (i < 6) i else if (i < 8) i + 1 else size - 15 + i
            modules[r][8] = bit
            // Horizontal placement
            val c = if (i < 8) size - 1 - i else if (i < 9) 15 - i else 14 - i
            modules[8][c] = bit
        }

        return QrMatrix(size, modules)
    }

    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val length: Int get() = bits.size

        fun append(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                bits.add(((value shr i) and 1) == 1)
            }
        }

        fun toByteArray(): ByteArray {
            val bytes = ByteArray((bits.size + 7) / 8)
            for (i in bits.indices) {
                if (bits[i]) {
                    bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
                }
            }
            return bytes
        }
    }
}
