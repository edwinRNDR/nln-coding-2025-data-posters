package lib

import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
fun hash(str: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(str.toByteArray())
    return digest.toHexString()
}