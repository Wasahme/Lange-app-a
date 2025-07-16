package com.securebluetooth.chat

import java.security.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

class CryptoManager {
    private val keyPairGenerator = KeyPairGenerator.getInstance("EC")
    private val keyAgreement = KeyAgreement.getInstance("ECDH")
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")

    fun generateKeyPair(): KeyPair {
        keyPairGenerator.initialize(256)
        return keyPairGenerator.generateKeyPair()
    }

    fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun decrypt(encryptedData: ByteArray, key: SecretKey): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encryptedData)
    }

    fun getSecretKeyFromBytes(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, 0, keyBytes.size, "AES")
    }
}