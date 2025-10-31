package com.example.nexus.data.repositories

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.PublicKey


import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
/**
 * The production implementation of our SecurityRepository.
 * This class uses Google Tink's modern primitives for secure operations.
 */
class SecurityRepositoryImpl(
    private val context: Context
) : SecurityRepository {

    companion object {
        private const val KEYSET_NAME = "nexa_master_keyset"
        private const val PREF_FILE = "nexa_security_prefs"
        private const val MASTER_KEY_URI = "android-keystore://_nexa_master_key_"
    }

    init {
        try {
            AeadConfig.register()
            HybridConfig.register()
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }
    }

    // --- Key Management ---

    private fun getOrCreatePrivateKeyHandle(): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
    }

    override fun generateKeyPair() {
        Log.d("SecurityRepo", "Initiating key pair generation process.")
        getOrCreatePrivateKeyHandle()
        Log.d("SecurityRepo", "Key pair generation process finished.")
    }

    override fun getMyPublicKeyBytes(): ByteArray? {
        Log.d("SecurityRepo", "Attempting to get public key...")
        return try {
            val publicKeysetHandle = getOrCreatePrivateKeyHandle().publicKeysetHandle
            val stream = ByteArrayOutputStream()
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(stream))
            val publicKeyBytes = stream.toByteArray()
            Log.d("SecurityRepo", "Public key retrieved successfully. Size: ${publicKeyBytes.size} bytes.")
            publicKeyBytes
        } catch (e: GeneralSecurityException) {
            Log.e("SecurityRepo", "FAILED to get public key. Error: ${e.message}", e)
            null
        }
    }

    // --- Encryption & Decryption (The Correct Way) ---

    /**
     * Encrypts plaintext using the recipient's public key.
     * This uses Tink's Hybrid Encryption which implicitly handles key agreement.
     */
    override fun encrypt(plaintext: String, theirPublicKeyBytes: ByteArray): ByteArray? {
        Log.d("SecurityRepo", "Attempting to encrypt plaintext. Plaintext size: ${plaintext.length} chars.")
        return try {
            val theirPublicKeysetHandle = CleartextKeysetHandle.read(
                JsonKeysetReader.withBytes(theirPublicKeyBytes))

            val hybridEncrypt = theirPublicKeysetHandle.getPrimitive(HybridEncrypt::class.java)

            val contextInfo = "nexa_handshake".toByteArray(StandardCharsets.UTF_8)

            val ciphertext = hybridEncrypt.encrypt(plaintext.toByteArray(StandardCharsets.UTF_8), contextInfo)
            Log.d("SecurityRepo", "Encryption successful. Ciphertext size: ${ciphertext.size} bytes.")
            ciphertext
        } catch (e: GeneralSecurityException) {
            Log.e("SecurityRepo", "Encryption FAILED. Error: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts ciphertext using our own private key.
     */
    override fun decrypt(ciphertext: ByteArray, contextInfo: ByteArray): String? {
        Log.d("SecurityRepo", "Attempting to decrypt ciphertext. Ciphertext size: ${ciphertext.size} bytes.")
        return try {
            val privateKeysetHandle = getOrCreatePrivateKeyHandle()

            val hybridDecrypt = privateKeysetHandle.getPrimitive(HybridDecrypt::class.java)

            val decryptedBytes = hybridDecrypt.decrypt(ciphertext, contextInfo)
            val decryptedString = String(decryptedBytes, StandardCharsets.UTF_8)
            Log.d("SecurityRepo", "Decryption successful. Decrypted plaintext size: ${decryptedString.length} chars.")
            decryptedString
        } catch (e: GeneralSecurityException) {
            Log.e("SecurityRepo", "Decryption FAILED. Error: ${e.message}", e)
            null
        }
    }
}