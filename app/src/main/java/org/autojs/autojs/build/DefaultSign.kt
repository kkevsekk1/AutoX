package org.autojs.autojs.build

import android.util.Base64
import com.stardust.pio.PFiles
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.*
import java.lang.Exception
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Modified by wilinz on 2022/5/23
 */
object DefaultSign {
    @Throws(UnsupportedEncodingException::class)
    private fun dBase64(data: String): ByteArray {
        return Base64.decode(data.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
    }

    @Throws(IOException::class)
    private fun doDir(
        prefix: String,
        dir: File,
        zos: ZipOutputStream,
        dos: DigestOutputStream,
        m: Manifest
    ) {
        zos.putNextEntry(ZipEntry(prefix))
        zos.closeEntry()
        val arrayOfFiles = dir.listFiles()!!
        val len = arrayOfFiles.size
        for (i in 0 until len) {
            val f = arrayOfFiles[i]
            if (f.isFile) {
                doFile(prefix + f.name, f, zos, dos, m)
            } else {
                doDir(prefix + f.name + "/", f, zos, dos, m)
            }
        }
    }

    @Throws(IOException::class)
    private fun doFile(
        name: String,
        f: File,
        zos: ZipOutputStream,
        dos: DigestOutputStream,
        m: Manifest
    ) {
        zos.putNextEntry(ZipEntry(name))
        val fis = FileUtils.openInputStream(f)
        IOUtils.copy(fis, dos)
        IOUtils.closeQuietly(fis)
        val digets = dos.messageDigest.digest()
        zos.closeEntry()
        val attr = Attributes()
        attr.putValue("SHA1-Digest", eBase64(digets))
        m.entries[name] = attr
    }

    private fun eBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    private fun generateSF(manifest: Manifest): Manifest {
        val md = MessageDigest.getInstance("SHA1")
        val print = PrintStream(DigestOutputStream(object : OutputStream() {
            override fun write(arg0: ByteArray) = Unit
            override fun write(arg0: ByteArray, arg1: Int, arg2: Int) = Unit
            override fun write(arg0: Int) = Unit
        }, md), true, "UTF-8")
        val sf = Manifest()
        val entries = manifest.entries
        val iterator: Iterator<Map.Entry<String, Attributes>> = entries.entries.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            print.print(
                """
    Name: $key
    
    """.trimIndent()
            )
            val iter: Iterator<Map.Entry<Any, Any>> = value.entries.iterator()
            while (iter.hasNext()) {
                val (key1, value1) = iter.next()
                print.print(
                    """
    $key1: $value1
    
    """.trimIndent()
                )
            }
            print.print("\r\n")
            print.flush()
            val sfAttr = Attributes()
            sfAttr.putValue("SHA1-Digest", eBase64(md.digest()))
            sf.entries[key] = sfAttr
        }
        return sf
    }

    @Throws(Exception::class)
    private fun instanceSignature(): Signature {
        val data =
            dBase64("MIIBVgIBADANBgkqhkiG9w0BAQEFAASCAUAwggE8AgEAAkEAoiZSqWnFDHA5sXKoDiUUO9JuL7cm/2dCck5MKumVvv+WfSg0jsovnywsFN0pifmdRSLmOdUkh0d0J+tOnSgtsQIDAQABAkEAihag5u3Qhds9BsViIUmqhZebhr8vUuqZR8cuTo1GnbSoOHIPbAgD3J8TDbC/CVqae8NrgwLp325Pem1Tuof/0QIhAN1hqft1K307bsljgw3iYKopGVZBHRXsjRnNL4edV9QrAiEAu4F+XtS1wohGLz5QtfuMFsQNo4l31mCjt6WpBDmSi5MCIQCB++YijxmJ3mueM5+vd0vqnVcTHghF5y6yB5fwuKHpIQIgInnS1Hjj2prX3MPmby+LOHxfzZvvDtnCAHhTNVWonkUCIQCvV8l+SpL6Vh1nQ/2EKFJo2dbZB3wKG/BEYsFkPFbn9w==")
        val rSAKeyFactory = KeyFactory.getInstance("RSA")
        val privateKey = rSAKeyFactory.generatePrivate(PKCS8EncodedKeySpec(data))
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(privateKey)
        return signature
    }

    @Throws(Exception::class)
    fun sign(dir: File, out: OutputStream) {
        File(dir, "META-INF").listFiles()?.forEach {
            if (it.extension.matches(Regex("MF|SF|RSA"))) it.delete()
        }
        val zos = ZipOutputStream(out)
        val manifest = Manifest()
        val sha1Manifest = writeMF(dir, manifest, zos)
        val sf = generateSF(manifest)
        val sign = writeSF(zos, sf, sha1Manifest)
        writeRSA(zos, sign)
        IOUtils.closeQuietly(zos)
    }

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    private fun writeMF(dir: File, manifest: Manifest, zos: ZipOutputStream): String {
        val md = MessageDigest.getInstance("SHA1")
        val dos = DigestOutputStream(zos, md)
        zipAndSha1(dir, zos, dos, manifest)
        val main = manifest.mainAttributes
        main.putValue("Manifest-Version", "1.0")
        main.putValue("Created-By", "Auto.js")
        zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
        manifest.write(dos)
        zos.closeEntry()
        return eBase64(md.digest())
    }

    @Throws(IOException::class)
    private fun writeRSA(zos: ZipOutputStream, sign: ByteArray) {
        zos.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
        zos.write(dBase64("MIIB5gYJKoZIhvcNAQcCoIIB1zCCAdMCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHAaCCATYwggEyMIHdoAMCAQICBCunMokwDQYJKoZIhvcNAQELBQAwDzENMAsGA1UEAxMEVGVzdDAeFw0xMjA0MjIwODQ1NDdaFw0xMzA0MjIwODQ1NDdaMA8xDTALBgNVBAMTBFRlc3QwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAoiZSqWnFDHA5sXKoDiUUO9JuL7cm/2dCck5MKumVvv+WfSg0jsovnywsFN0pifmdRSLmOdUkh0d0J+tOnSgtsQIDAQABoyEwHzAdBgNVHQ4EFgQUVL2yOinUwpARE1tOPxc1bf4WrTgwDQYJKoZIhvcNAQELBQADQQAnj/eZwhqwb2tgSYNvgRo5bBNNCpJbQ4alEeP/MLSIWf2nZpAix8T3oS9X2affQtAgctPATcKQaiH2B4L7FKlVMXoweAIBATAXMA8xDTALBgNVBAMTBFRlc3QCBCunMokwCQYFKw4DAhoFADANBgkqhkiG9w0BAQEFAARA"))
        zos.write(sign)
        zos.closeEntry()
    }

    @Throws(Exception::class)
    private fun writeSF(zos: ZipOutputStream, sf: Manifest, sha1Manifest: String): ByteArray {
        val signature = instanceSignature()
        zos.putNextEntry(ZipEntry("META-INF/CERT.SF"))
        val out = SignatureOutputStream(zos, signature)
        out.write("Signature-Version: 1.0\r\n".toByteArray(charset("UTF-8")))
        out.write(
            """Created-By: tiny-sign-${DefaultSign::class.java.getPackage()?.implementationVersion}
""".toByteArray(charset("UTF-8"))
        )
        out.write("SHA1-Digest-Manifest: ".toByteArray(charset("UTF-8")))
        out.write(sha1Manifest.toByteArray(charset("UTF-8")))
        out.write(13)
        out.write(10)
        sf.write(out)
        zos.closeEntry()
        return signature.sign()
    }

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    private fun zipAndSha1(dir: File, zos: ZipOutputStream, dos: DigestOutputStream, m: Manifest) {
        val metaDir = File(dir, "META-INF")
        if (!metaDir.exists()) metaDir.mkdirs()
        val children = dir.listFiles() ?: return
        for (element in children) {
            if (!PFiles.getExtension(element.name).matches(Regex("MF|RSA|SF"))) {
                if (element.isFile) {
                    doFile(element.name, element, zos, dos, m)
                } else {
                    doDir(element.name + "/", element, zos, dos, m)
                }
            }
        }
    }

    private class SignatureOutputStream(out: OutputStream?, private val mSignature: Signature) :
        FilterOutputStream(out) {
        @Throws(IOException::class)
        override fun write(buffer: ByteArray) {
            try {
                mSignature.update(buffer)
            } catch (var3: SignatureException) {
                throw IOException("SignatureException: $var3")
            }
            out.write(buffer)
        }

        @Throws(IOException::class)
        override fun write(b: ByteArray, off: Int, len: Int) {
            try {
                mSignature.update(b, off, len)
            } catch (var5: SignatureException) {
                throw IOException("SignatureException: $var5")
            }
            out.write(b, off, len)
        }

        @Throws(IOException::class)
        override fun write(b: Int) {
            try {
                mSignature.update(b.toByte())
            } catch (var3: SignatureException) {
                throw IOException("SignatureException: $var3")
            }
            out.write(b)
        }
    }
}