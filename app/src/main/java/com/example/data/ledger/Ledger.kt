package com.example.data.ledger

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.BuildConfig
import com.example.data.model.RepaymentRecord
import java.io.File
import java.net.URL
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToLong

class OfficerSession(val officer: String, val token: String, val expires: Long) {
    fun valid() = System.currentTimeMillis() / 1000 < expires
    companion object {
        fun parse(json: JSONObject) = OfficerSession(json.getString("officer"), json.getString("token"), json.getLong("expires"))
    }
}

object Ledger {
    var session: OfficerSession? = null
        private set

    private const val alias = "saptadhan.biometric.session.v1"
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun file(context: Context) = File(context.noBackupFilesDir, "officer.session")
    private fun cipher() = Cipher.getInstance("RSA/ECB/PKCS1Padding")

    private suspend fun post(path: String, body: JSONObject, token: String? = null) = withContext(Dispatchers.IO) {
        val base = BuildConfig.LEDGER_API_URL.trimEnd('/')
        require(base.startsWith("https://") && base != "https://unconfigured.invalid") { "Configure the HTTPS ledger address in the app build." }
        val conn = URL("$base/$path").openConnection() as HttpsURLConnection
        try {
            conn.requestMethod = "POST"
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            check(conn.responseCode == 200) {
                when (conn.responseCode) {
                    401 -> "Invalid credentials or expired session. Sign in with your officer PIN."
                    429 -> "Too many attempts. Try again in five minutes."
                    else -> "Ledger request failed (HTTP ${conn.responseCode})."
                }
            }
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    suspend fun login(context: Context, officer: String, pin: String, remember: Boolean): OfficerSession {
        val json = post("login", JSONObject().put("officer", officer.trim()).put("pin", pin))
        val verified = OfficerSession.parse(json)
        logout(context)
        if (remember) runCatching {
            val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
            generator.initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(3072).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(true).build())
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(generator.generateKeyPair().public.encoded))
            val encrypt = cipher().apply { init(Cipher.ENCRYPT_MODE, publicKey) }
            file(context).writeBytes(encrypt.doFinal(json.toString().toByteArray(Charsets.UTF_8)))
        }
        session = verified
        return verified
    }

    fun biometric(activity: FragmentActivity, success: (OfficerSession) -> Unit, fail: (String) -> Unit) {
        try {
            val encrypted = file(activity).readBytes()
            val decrypt = cipher().apply { init(Cipher.DECRYPT_MODE, store().getKey(alias, null)) }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(code: Int, message: CharSequence) = fail(message.toString())
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val verified = runCatching {
                            val bytes = requireNotNull(result.cryptoObject?.cipher).doFinal(encrypted)
                            OfficerSession.parse(JSONObject(String(bytes, Charsets.UTF_8))).also { check(it.valid()) }
                        }.getOrNull()
                        if (verified == null) fail("Sign in again with your officer PIN.")
                        else { session = verified; success(verified) }
                    }
                })
            val info = BiometricPrompt.PromptInfo.Builder().setTitle("Unlock officer account")
                .setAllowedAuthenticators(BIOMETRIC_STRONG).setNegativeButtonText("Cancel").build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(decrypt))
        } catch (_: Exception) {
            fail("Sign in online with Remember me enabled and enroll a strong device biometric.")
        }
    }

    fun logout(context: Context) {
        session = null
        file(context).delete()
        store().apply { if (containsAlias(alias)) deleteEntry(alias) }
    }

    fun active() = requireNotNull(session?.takeIf { it.valid() }) { "Sign in again with your officer PIN." }

    suspend fun disbursements() = post("disbursements", JSONObject(), active().token).getJSONObject("groups")

    suspend fun upload(r: RepaymentRecord) {
        val officer = active()
        check(r.officerCode == officer.officer)
        val body = JSONObject().put("receipt", r.receiptNumber).put("officer", r.officerCode)
            .put("borrower", r.borrowerId).put("group", r.groupId).put("center", r.centerId)
            .put("mode", r.paymentMode.name).put("timestamp", r.timestamp)
            .put("amountPaise", (r.amountPaid * 100).roundToLong()).put("attendance", r.attendance.name)
            .put("upiTxnId", r.upiTxnId).put("overdueReason", r.overdueReason?.name)
            .put("guarantee", r.isCoveredByGuarantee)
        val ack = post("payments", body, officer.token)
        check(ack.getBoolean("saved") && ack.getString("receipt") == r.receiptNumber) { "Invalid ledger receipt acknowledgment." }
    }
}
