package com.example.nexus.data.repositories

import java.security.PrivateKey
import java.security.PublicKey

/**
 * An interface for the repository that handles all cryptographic operations.
 * This is the "encryption engine" of the application. Its responsibilities include
 * key generation, secure storage, key exchange computations, and data encryption/decryption.
 */
interface SecurityRepository {

    /**
     * Generates a new cryptographic key pair (private and public) for the user.
     * The private key MUST be stored securely in the Android Keystore.
     * The public key can be stored in regular app storage (e.g., SharedPreferences or Room).
     */
    fun generateKeyPair()

    /**
     * Retrieves the user's own public key from storage.
     * This is the key that will be shared with other peers during a handshake.
     * @return The user's public key, or null if it doesn't exist.
     */
    fun getMyPublicKeyBytes(): ByteArray?

    /**
     * Encrypts a plain text string using a previously established shared secret.
     * @param plaintext The message to be encrypted.
     * @param theirPublicKey The public key received from the other peer.
     * @return An encrypted ByteArray, or null on failure.
     */
    fun encrypt(plaintext: String, theirPublicKeyBytes: ByteArray): ByteArray?

    /**
     * Decrypts an encrypted ByteArray using a previously established shared secret.
     * @param ciphertext The encrypted message data received from a peer.
     * @param contextInfo A unique identifier for the session.
     * @return The original plain text string, or null on failure (e.g., wrong key).
     */
    fun decrypt(ciphertext: ByteArray, contextInfo: ByteArray): String?

}