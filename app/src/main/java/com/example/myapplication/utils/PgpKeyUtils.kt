package com.example.myapplication.utils


import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.*
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.Security
import java.util.*

object gpKeyUtils {

    init {
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }

    fun generatePGPKeyPair(identity: String, password: String): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val rsaKeyPair = keyGen.generateKeyPair()

        val sha256Calc = JcaPGPDigestCalculatorProviderBuilder()
            .setProvider("BC").build().get(HashAlgorithmTags.SHA256)

        val keyPair = JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, rsaKeyPair, Date())

        val signerBuilder = JcaPGPContentSignerBuilder(
            keyPair.publicKey.algorithm, HashAlgorithmTags.SHA256
        ).setProvider("BC")

        val secretKeyEncryptor = JcePBESecretKeyEncryptorBuilder(
            SymmetricKeyAlgorithmTags.AES_256, sha256Calc
        ).setProvider("BC").build(password.toCharArray())

        val subpacketGen = PGPSignatureSubpacketGenerator().apply {
            setKeyFlags(false, KeyFlags.ENCRYPT_COMMS or KeyFlags.SIGN_DATA)
        }

        val keyRingGen = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            keyPair,
            identity,
            sha256Calc,
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
    }
}
