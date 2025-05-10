package com.example.myapplication.data.model.websocket

import android.util.Log
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.*
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.*

object PGPUtils {

    init {
        Security.removeProvider("BC")
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generatePGPKeyPair(identity: String, password: String): Pair<String, String> {
        try {
            val keyGen = KeyPairGenerator.getInstance("RSA", "BC")
            keyGen.initialize(2048)
            val rsaKeyPair = keyGen.generateKeyPair()

            val sha1Calc = JcaPGPDigestCalculatorProviderBuilder()
                .setProvider("BC").build().get(HashAlgorithmTags.SHA1)

            val keyPair = JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, rsaKeyPair, Date())

            val signerBuilder = JcaPGPContentSignerBuilder(
                keyPair.publicKey.algorithm, HashAlgorithmTags.SHA256
            ).setProvider("BC")

            val secretKeyEncryptor = JcePBESecretKeyEncryptorBuilder(
                SymmetricKeyAlgorithmTags.AES_256, sha1Calc
            ).setProvider("BC").build(password.toCharArray())

            val subpacketGen = PGPSignatureSubpacketGenerator().apply {
                setKeyFlags(false, KeyFlags.ENCRYPT_COMMS or KeyFlags.SIGN_DATA)
            }

            val keyRingGen = PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                keyPair,
                identity,
                sha1Calc,
                subpacketGen.generate(),
                null,
                signerBuilder,
                secretKeyEncryptor
            )

            val privateOut = ByteArrayOutputStream()
            ArmoredOutputStream(privateOut).use { aos ->
                keyRingGen.generateSecretKeyRing().encode(aos)
            }

            val publicOut = ByteArrayOutputStream()
            ArmoredOutputStream(publicOut).use { aos ->
                keyRingGen.generatePublicKeyRing().encode(aos)
            }

            return Pair(publicOut.toString("UTF-8"), privateOut.toString("UTF-8"))
        } catch (e: Exception) {
            Log.e("PGPUtils", "Key generation failed", e)
            throw RuntimeException("Failed to generate PGP keys", e)
        }
    }

    fun encryptMessage(message: String, publicKeyArmored: String): String {
        try {
            if (!publicKeyArmored.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
                throw IllegalArgumentException("Invalid public key format")
            }

            val pgpPubKeyRing = PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKeyArmored.byteInputStream(StandardCharsets.UTF_8)),
                JcaKeyFingerprintCalculator()
            )

            val publicKey = pgpPubKeyRing.keyRings.asSequence()
                .flatMap { it.publicKeys.asSequence() }
                .firstOrNull { it.isEncryptionKey }
                ?: throw IllegalArgumentException("No suitable encryption key found")

            val out = ByteArrayOutputStream()
            ArmoredOutputStream(out).use { armoredOut ->
                val encryptorBuilder = JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                    .setWithIntegrityPacket(true)
                    .setSecureRandom(SecureRandom())
                    .setProvider("BC")

                val generator = PGPEncryptedDataGenerator(encryptorBuilder)
                generator.addMethod(
                    JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC")
                )

                val encOut = generator.open(armoredOut, ByteArray(4096))

                // Create literal data generator
                val literalDataGenerator = PGPLiteralDataGenerator()
                try {
                    val literalOut = literalDataGenerator.open(
                        encOut,
                        PGPLiteralData.BINARY,
                        "message",
                        message.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                        Date()
                    )

                    literalOut.use {
                        it.write(message.toByteArray(StandardCharsets.UTF_8))
                    }
                } finally {
                    literalDataGenerator.close()
                }

                encOut.close()
            }

            val encrypted = out.toString("UTF-8")
            Log.d("PGPUtils", "Encrypted message:\n$encrypted")
            return encrypted
        } catch (e: Exception) {
            Log.e("PGPUtils", "Encryption failed", e)
            throw RuntimeException("Failed to encrypt message", e)
        }
    }

    fun decryptMessage(encryptedMessageArmored: String, privateKeyArmored: String, password: String): String {
        try {
            if (!encryptedMessageArmored.contains("-----BEGIN PGP MESSAGE-----")) {
                throw PGPException("Invalid PGP message format - missing header")
            }

            if (!privateKeyArmored.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----")) {
                throw PGPException("Invalid private key format")
            }

            Log.d("PGPUtils", "Starting decryption...")

            val pgpObjectFactory = PGPObjectFactory(
                PGPUtil.getDecoderStream(encryptedMessageArmored.byteInputStream(StandardCharsets.UTF_8)),
                JcaKeyFingerprintCalculator()
            )

            // Debug: Log all objects in the stream
            var obj = pgpObjectFactory.nextObject()
            while (obj != null) {
                Log.d("PGPUtils", "Found object: ${obj::class.java.simpleName}")
                obj = pgpObjectFactory.nextObject()
            }

            // Reset the factory to read again
            val freshFactory = PGPObjectFactory(
                PGPUtil.getDecoderStream(encryptedMessageArmored.byteInputStream(StandardCharsets.UTF_8)),
                JcaKeyFingerprintCalculator()
            )

            val encryptedDataList = generateSequence { freshFactory.nextObject() }
                .filterIsInstance<PGPEncryptedDataList>()
                .firstOrNull() ?: throw PGPException("No encrypted data found")

            val secretKeyRingCollection = PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(privateKeyArmored.byteInputStream(StandardCharsets.UTF_8)),
                JcaKeyFingerprintCalculator()
            )

            var privateKey: PGPPrivateKey? = null
            var encData: PGPPublicKeyEncryptedData? = null

            for (encryptedData in encryptedDataList.encryptedDataObjects) {
                if (encryptedData is PGPPublicKeyEncryptedData) {
                    val secretKey = secretKeyRingCollection.getSecretKey(encryptedData.keyID) ?: continue
                    privateKey = secretKey.extractPrivateKey(
                        JcePBESecretKeyDecryptorBuilder()
                            .setProvider("BC")
                            .build(password.toCharArray())
                    )
                    encData = encryptedData
                    break
                }
            }

            if (privateKey == null || encData == null) {
                throw PGPException("Private key not found or password incorrect")
            }

            val clearStream = encData.getDataStream(
                JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider("BC").build(privateKey)
            )

            val plainFactory = PGPObjectFactory(clearStream, JcaKeyFingerprintCalculator())
            val message = plainFactory.nextObject()

            val literalData = when (message) {
                is PGPLiteralData -> message
                is PGPCompressedData -> {
                    val compressedFactory = PGPObjectFactory(message.dataStream, JcaKeyFingerprintCalculator())
                    compressedFactory.nextObject() as? PGPLiteralData
                }
                else -> null
            } ?: throw PGPException("Unexpected message type")

            val decrypted = literalData.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
            Log.d("PGPUtils", "Decryption successful")
            return decrypted
        } catch (e: Exception) {
            Log.e("PGPUtils", "Decryption failed", e)
            throw RuntimeException("Failed to decrypt message", e)
        }
    }
}