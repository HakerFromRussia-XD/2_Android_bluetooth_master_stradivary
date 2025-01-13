package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlin.experimental.xor

class CrcCalc {
    companion object{
        fun crcCalc(data: ByteArray): Byte {
            var countLocal = data.size - 1
            val crcTable = byteArrayOf(
                0, 94, 188.toByte(), 226.toByte(), 97, 63, 221.toByte(), 131.toByte(), 194.toByte(), 156.toByte(), 126, 32, 163.toByte(), 253.toByte(), 31, 65,
                157.toByte(), 195.toByte(), 33, 127, 252.toByte(), 162.toByte(), 64, 30, 95, 1, 227.toByte(), 189.toByte(), 62, 96, 130.toByte(), 220.toByte(),
                35, 125, 159.toByte(), 193.toByte(), 66, 28, 254.toByte(), 160.toByte(), 225.toByte(), 191.toByte(), 93, 3, 128.toByte(), 222.toByte(), 60, 98,
                190.toByte(), 224.toByte(), 2, 92, 223.toByte(), 129.toByte(), 99, 61, 124, 34, 192.toByte(), 158.toByte(), 29, 67, 161.toByte(), 255.toByte(),
                70, 24, 250.toByte(), 164.toByte(), 39, 121, 155.toByte(), 197.toByte(), 132.toByte(), 218.toByte(), 56, 102, 229.toByte(), 187.toByte(), 89, 7,
                219.toByte(), 133.toByte(), 103, 57, 186.toByte(), 228.toByte(), 6, 88, 25, 71, 165.toByte(), 251.toByte(), 120, 38, 196.toByte(), 154.toByte(),
                101, 59, 217.toByte(), 135.toByte(), 4, 90, 184.toByte(), 230.toByte(), 167.toByte(), 249.toByte(), 27, 69, 198.toByte(), 152.toByte(), 122, 36,
                248.toByte(), 166.toByte(), 68, 26, 153.toByte(), 199.toByte(), 37, 123, 58, 100, 134.toByte(), 216.toByte(), 91, 5, 231.toByte(), 185.toByte(),
                140.toByte(), 210.toByte(), 48, 110, 237.toByte(), 179.toByte(), 81, 15, 78, 16, 242.toByte(), 172.toByte(), 47, 113, 147.toByte(), 205.toByte(),
                17, 79, 173.toByte(), 243.toByte(), 112, 46, 204.toByte(), 146.toByte(), 211.toByte(), 141.toByte(), 111, 49, 178.toByte(), 236.toByte(), 14, 80,
                175.toByte(), 241.toByte(), 19, 77, 206.toByte(), 144.toByte(), 114, 44, 109, 51, 209.toByte(), 143.toByte(), 12, 82, 176.toByte(), 238.toByte(),
                50, 108, 142.toByte(), 208.toByte(), 83, 13, 239.toByte(), 177.toByte(), 240.toByte(), 174.toByte(), 76, 18, 145.toByte(), 207.toByte(), 45, 115,
                202.toByte(), 148.toByte(), 118, 40, 171.toByte(), 245.toByte(), 23, 73, 8, 86, 180.toByte(), 234.toByte(), 105, 55, 213.toByte(), 139.toByte(),
                87, 9, 235.toByte(), 181.toByte(), 54, 104, 138.toByte(), 212.toByte(), 149.toByte(), 203.toByte(), 41, 119, 244.toByte(), 170.toByte(), 72, 22,
                233.toByte(), 183.toByte(), 85, 11, 136.toByte(), 214.toByte(), 52, 106, 43, 117, 151.toByte(), 201.toByte(), 74, 20, 246.toByte(), 168.toByte(),
                116, 42, 200.toByte(), 150.toByte(), 21, 75, 169.toByte(), 247.toByte(), 182.toByte(), 232.toByte(), 10, 84, 215.toByte(), 137.toByte(), 107, 53
            )
            var result: Byte = 0
            var i = 0
            while (countLocal != 0 ) {
                result = crcTable[castUnsignedCharToInt(result xor data[i])]
                i++
                countLocal--
            }
            return result
        }
    }
}