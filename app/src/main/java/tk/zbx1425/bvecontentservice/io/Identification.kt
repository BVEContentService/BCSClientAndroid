import android.util.Base64
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.io.hThread
import java.io.File
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


/**
 * @author liangjun on 2018/1/21.
 * Copied-pasted from CSDN
 */
object Identification {
    val idFileName = "." + PackLocalManager.encodeInvisibleString("INSTALLATION") + "thumbnails"
    private lateinit var installationID: String
    private val utf8 = Charset.forName("UTF-8")
    private val internalFile = File(ApplicationContext.context.filesDir, idFileName)
    private val externalFileA = File(PackLocalManager.appDir, idFileName)
    private val externalFileH = File(PackLocalManager.hmmDir, idFileName)

    val deviceID: String get(){
        if (::installationID.isInitialized) return installationID
        installationID = readID()
        return installationID
    }

    fun getDateChecksum(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHH", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val inputString = String.format("%s114514%s1919.810", deviceID, dateFormat.format(Date()))
        return toBase64(MessageDigest.getInstance("md5").digest(inputString.toByteArray(utf8)))
    }

    fun getChecksum(metadata: PackageMetadata): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHH", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val inputString = String.format("%sB%sC%sS%s114514-1919.810", metadata.ID,
            metadata.Author.ID, deviceID, dateFormat.format(Date()))
        return toBase64(MessageDigest.getInstance("md5").digest(inputString.toByteArray(utf8)))
    }

    private fun readID(): String{
        val ID: String = readFromFile(internalFile) ?: readFromFile(externalFileA)
            ?: readFromFile(externalFileH) ?: UUID.randomUUID().toString()
        writeToFile(internalFile, ID)
        writeToFile(externalFileA, ID)
        writeToFile(externalFileH, ID)
        return ID
    }

    private fun readFromFile(file: File): String?{
        if (!file.exists()) return null
        val lines = file.readLines(Charset.forName("utf-8"))
        if (lines.count() < 3) return null
        val payload = decrypt(lines[0], lines[2]) ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(utf8))
        if (toBase64(digest) != lines[1]) return null
        return payload
    }

    private fun writeToFile(file: File, payload: String){
        file.parentFile?.mkdirs()
        val key = UUID.randomUUID().toString()
        val encrypted = encrypt(payload, key)
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(utf8))
        val content = String.format("%s\n%s\n%s\n", encrypted, toBase64(digest), key)
        file.writeText(content, utf8)
    }

    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 256
    private const val PBKDF2_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val PKCS5_SALT_LENGTH = 32
    private const val DELIMITER = "]"
    private val random = SecureRandom()

    fun encrypt(plaintext: String, password: String): String? {
        val salt = generateSalt()
        val key: SecretKey = deriveKey(password, salt)
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val iv = generateIv(cipher.blockSize)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams)
            val cipherText: ByteArray = cipher.doFinal(plaintext.toByteArray(utf8))
            if (salt != null) {
                String.format(
                    "%s%s%s%s%s",
                    toBase64(salt),
                    DELIMITER,
                    toBase64(iv),
                    DELIMITER,
                    toBase64(cipherText)
                )
            } else String.format(
                "%s%s%s",
                toBase64(iv),
                DELIMITER,
                toBase64(cipherText)
            )
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            return null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }
    }

    fun decrypt(ciphertext: String, password: String): String? {
        val fields = ciphertext.split(DELIMITER).toTypedArray()
        require(fields.size == 3) { "Invalid encypted text format" }
        val salt = fromBase64(fields[0])
        val iv = fromBase64(fields[1])
        val cipherBytes = fromBase64(fields[2])
        val key: SecretKey = deriveKey(password, salt)
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val ivParams = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams)
            val plaintext: ByteArray = cipher.doFinal(cipherBytes)
            String(plaintext, utf8)
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            return null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }
    }

    private fun generateSalt(): ByteArray? {
        val b = ByteArray(PKCS5_SALT_LENGTH)
        random.nextBytes(b)
        return b
    }

    private fun generateIv(length: Int): ByteArray {
        val b = ByteArray(length)
        random.nextBytes(b)
        return b
    }

    private fun deriveKey(password: String, salt: ByteArray?): SecretKey {
        return try {
            val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val keyFactory= SecretKeyFactory.getInstance(PBKDF2_DERIVATION_ALGORITHM)
            val keyBytes: ByteArray = keyFactory.generateSecret(keySpec).encoded
            SecretKeySpec(keyBytes, "AES")
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }
    }

    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun fromBase64(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }

    val IPAddress: String
        get() {
            val latch = CountDownLatch(1)
            var response = ""
            hThread {
                try {
                    response = HttpHelper.fetchString("http://ipv4.icanhazip.com") ?: ""
                } catch (ex: Exception) {
                }
                latch.countDown()
            }.start()
            latch.await()
            return response
        }
}