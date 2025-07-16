package com.bluetoothchat.encrypted.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.security.KeyPair
import java.security.SecureRandom
import javax.crypto.SecretKey

/**
 * Unit tests for CryptoManager
 * Tests encryption, decryption, key exchange, and security features
 */
class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager
    private lateinit var secureRandom: SecureRandom

    @Before
    fun setUp() {
        cryptoManager = CryptoManager()
        secureRandom = SecureRandom()
    }

    @Test
    fun testECDHKeyPairGeneration() {
        // Test key pair generation
        val keyPair = cryptoManager.generateECDHKeyPair()
        
        assertNotNull("Public key should not be null", keyPair.public)
        assertNotNull("Private key should not be null", keyPair.private)
        assertEquals("Key algorithm should be EC", "EC", keyPair.public.algorithm)
        assertEquals("Key algorithm should be EC", "EC", keyPair.private.algorithm)
    }

    @Test
    fun testSharedSecretDerivation() {
        // Generate two key pairs
        val keyPair1 = cryptoManager.generateECDHKeyPair()
        val keyPair2 = cryptoManager.generateECDHKeyPair()
        
        // Derive shared secrets
        val sharedSecret1 = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair2.public)
        val sharedSecret2 = cryptoManager.deriveSharedSecret(keyPair2.private, keyPair1.public)
        
        // Shared secrets should be identical
        assertArrayEquals("Shared secrets should be identical", sharedSecret1, sharedSecret2)
        assertTrue("Shared secret should not be empty", sharedSecret1.isNotEmpty())
    }

    @Test
    fun testSessionKeyDerivation() {
        // Generate shared secret
        val keyPair1 = cryptoManager.generateECDHKeyPair()
        val keyPair2 = cryptoManager.generateECDHKeyPair()
        val sharedSecret = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair2.public)
        
        // Generate salt
        val salt = cryptoManager.generateSalt()
        assertEquals("Salt should be 16 bytes", 16, salt.size)
        
        // Derive session keys
        val (aesKey, hmacKey) = cryptoManager.deriveSessionKeys(sharedSecret, salt)
        
        assertNotNull("AES key should not be null", aesKey)
        assertNotNull("HMAC key should not be null", hmacKey)
        assertEquals("AES key algorithm should be AES", "AES", aesKey.algorithm)
        assertEquals("HMAC key algorithm should be HmacSHA256", "HmacSHA256", hmacKey.algorithm)
    }

    @Test
    fun testMessageEncryptionDecryption() {
        // Setup session keys
        val keyPair1 = cryptoManager.generateECDHKeyPair()
        val keyPair2 = cryptoManager.generateECDHKeyPair()
        val sharedSecret = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair2.public)
        val salt = cryptoManager.generateSalt()
        val (aesKey, hmacKey) = cryptoManager.deriveSessionKeys(sharedSecret, salt)
        
        cryptoManager.setSessionKeys(aesKey, hmacKey)
        
        // Test message encryption and decryption
        val originalMessage = "Hello, this is a test message!"
        val messageBytes = originalMessage.toByteArray()
        
        // Encrypt message
        val encryptedMessage = cryptoManager.encryptMessage(messageBytes, MessageType.TEXT_MESSAGE)
        
        assertNotNull("Encrypted message should not be null", encryptedMessage)
        assertEquals("Message type should be TEXT_MESSAGE", MessageType.TEXT_MESSAGE, encryptedMessage.messageType)
        assertTrue("Ciphertext should not be empty", encryptedMessage.ciphertext.isNotEmpty())
        assertTrue("IV should not be empty", encryptedMessage.iv.isNotEmpty())
        assertTrue("Auth tag should not be empty", encryptedMessage.authTag.isNotEmpty())
        
        // Decrypt message
        val decryptedBytes = cryptoManager.decryptMessage(encryptedMessage)
        val decryptedMessage = String(decryptedBytes)
        
        assertEquals("Decrypted message should match original", originalMessage, decryptedMessage)
    }

    @Test
    fun testEncryptionWithDifferentMessageTypes() {
        // Setup session keys
        setupSessionKeys()
        
        val testData = "Test data".toByteArray()
        
        // Test different message types
        val messageTypes = listOf(
            MessageType.TEXT_MESSAGE,
            MessageType.VOICE_DATA,
            MessageType.PING,
            MessageType.ACK
        )
        
        messageTypes.forEach { messageType ->
            val encrypted = cryptoManager.encryptMessage(testData, messageType)
            val decrypted = cryptoManager.decryptMessage(encrypted)
            
            assertEquals("Message type should be preserved", messageType, encrypted.messageType)
            assertArrayEquals("Decrypted data should match original for $messageType", testData, decrypted)
        }
    }

    @Test
    fun testSequenceNumberIncrement() {
        setupSessionKeys()
        
        val testData = "Test".toByteArray()
        
        // Encrypt multiple messages
        val message1 = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        val message2 = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        val message3 = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        
        // Sequence numbers should increment
        assertTrue("Sequence number should increment", message2.sequenceNumber > message1.sequenceNumber)
        assertTrue("Sequence number should increment", message3.sequenceNumber > message2.sequenceNumber)
    }

    @Test
    fun testRandomSaltGeneration() {
        val salt1 = cryptoManager.generateSalt()
        val salt2 = cryptoManager.generateSalt()
        
        assertEquals("Salt should be 16 bytes", 16, salt1.size)
        assertEquals("Salt should be 16 bytes", 16, salt2.size)
        assertFalse("Salts should be different", salt1.contentEquals(salt2))
    }

    @Test
    fun testRandomBytesGeneration() {
        val bytes1 = cryptoManager.generateRandomBytes(32)
        val bytes2 = cryptoManager.generateRandomBytes(32)
        
        assertEquals("Should generate requested number of bytes", 32, bytes1.size)
        assertEquals("Should generate requested number of bytes", 32, bytes2.size)
        assertFalse("Random bytes should be different", bytes1.contentEquals(bytes2))
    }

    @Test
    fun testLargeMessageEncryption() {
        setupSessionKeys()
        
        // Test with large message (10KB)
        val largeMessage = ByteArray(10 * 1024) { it.toByte() }
        
        val encrypted = cryptoManager.encryptMessage(largeMessage, MessageType.TEXT_MESSAGE)
        val decrypted = cryptoManager.decryptMessage(encrypted)
        
        assertArrayEquals("Large message should be encrypted and decrypted correctly", largeMessage, decrypted)
    }

    @Test
    fun testEmptyMessageEncryption() {
        setupSessionKeys()
        
        val emptyMessage = ByteArray(0)
        
        val encrypted = cryptoManager.encryptMessage(emptyMessage, MessageType.TEXT_MESSAGE)
        val decrypted = cryptoManager.decryptMessage(encrypted)
        
        assertArrayEquals("Empty message should be handled correctly", emptyMessage, decrypted)
    }

    @Test(expected = CryptoException::class)
    fun testEncryptionWithoutSessionKeys() {
        // Try to encrypt without setting session keys
        val testData = "Test".toByteArray()
        cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
    }

    @Test(expected = CryptoException::class)
    fun testDecryptionWithoutSessionKeys() {
        // Setup keys, encrypt, then clear keys and try to decrypt
        setupSessionKeys()
        val testData = "Test".toByteArray()
        val encrypted = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        
        cryptoManager.clearSession()
        cryptoManager.decryptMessage(encrypted)
    }

    @Test
    fun testSessionClearing() {
        setupSessionKeys()
        
        // Encrypt a message to verify keys are set
        val testData = "Test".toByteArray()
        val encrypted = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        assertNotNull("Should be able to encrypt with session keys", encrypted)
        
        // Clear session
        cryptoManager.clearSession()
        
        // Should not be able to encrypt after clearing
        try {
            cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
            fail("Should throw exception after clearing session")
        } catch (e: CryptoException) {
            // Expected
        }
    }

    @Test
    fun testKeyRotation() {
        setupSessionKeys()
        
        // Encrypt with first session
        val testData = "Test".toByteArray()
        val encrypted1 = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        
        // Rotate keys
        assertTrue("Key rotation should succeed", cryptoManager.rotateSessionKeys())
        
        // Note: In a real implementation, this would involve a new key exchange
        // For now, we just test that the rotation method works
    }

    @Test
    fun testConcurrentEncryption() {
        setupSessionKeys()
        
        val testData = "Concurrent test".toByteArray()
        val results = mutableListOf<EncryptedMessage>()
        
        // Simulate concurrent encryption (though not truly concurrent in unit test)
        repeat(100) {
            val encrypted = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
            results.add(encrypted)
        }
        
        // All messages should be encrypted successfully
        assertEquals("Should encrypt all messages", 100, results.size)
        
        // All messages should decrypt correctly
        results.forEach { encrypted ->
            val decrypted = cryptoManager.decryptMessage(encrypted)
            assertArrayEquals("All messages should decrypt correctly", testData, decrypted)
        }
        
        // Sequence numbers should be unique and incrementing
        val sequenceNumbers = results.map { it.sequenceNumber }
        assertEquals("All sequence numbers should be unique", sequenceNumbers.toSet().size, sequenceNumbers.size)
    }

    @Test
    fun testDifferentKeyPairsProduceDifferentSecrets() {
        val keyPair1 = cryptoManager.generateECDHKeyPair()
        val keyPair2 = cryptoManager.generateECDHKeyPair()
        val keyPair3 = cryptoManager.generateECDHKeyPair()
        
        val secret1 = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair2.public)
        val secret2 = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair3.public)
        
        assertFalse("Different key pairs should produce different secrets", secret1.contentEquals(secret2))
    }

    @Test
    fun testMessageIntegrity() {
        setupSessionKeys()
        
        val testData = "Integrity test".toByteArray()
        val encrypted = cryptoManager.encryptMessage(testData, MessageType.TEXT_MESSAGE)
        
        // Tamper with ciphertext
        val tamperedEncrypted = encrypted.copy(
            ciphertext = encrypted.ciphertext.copyOf().apply { this[0] = (this[0] + 1).toByte() }
        )
        
        // Should fail to decrypt tampered message
        try {
            cryptoManager.decryptMessage(tamperedEncrypted)
            fail("Should fail to decrypt tampered message")
        } catch (e: CryptoException) {
            // Expected - message integrity check failed
        }
    }

    /**
     * Helper method to set up session keys for testing
     */
    private fun setupSessionKeys() {
        val keyPair1 = cryptoManager.generateECDHKeyPair()
        val keyPair2 = cryptoManager.generateECDHKeyPair()
        val sharedSecret = cryptoManager.deriveSharedSecret(keyPair1.private, keyPair2.public)
        val salt = cryptoManager.generateSalt()
        val (aesKey, hmacKey) = cryptoManager.deriveSessionKeys(sharedSecret, salt)
        cryptoManager.setSessionKeys(aesKey, hmacKey)
    }
}