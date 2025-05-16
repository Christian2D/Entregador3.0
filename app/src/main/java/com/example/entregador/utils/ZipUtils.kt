package com.example.entregador.utils

import android.util.Log
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipUtils {
    fun unzipFromAssets(zipInputStream: InputStream, targetDirectory: File) {
        val start = System.currentTimeMillis()
        Log.d("ZipUtils", "Iniciando descompactação para: ${targetDirectory.absolutePath}")

        ZipInputStream(zipInputStream).use { zis ->
            var entry: ZipEntry?

            while (zis.nextEntry.also { entry = it } != null) {
                entry?.let { zipEntry ->
                    val newFile = File(targetDirectory, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        newFile.outputStream().use { output ->
                            zis.copyTo(output)
                        }
                        Log.d("ZipUtils", "Arquivo extraído: ${newFile.name}")
                    }
                }
            }
        }

        val arquivos = targetDirectory.listFiles()?.joinToString { it.name } ?: "Nenhum"
        val tempo = System.currentTimeMillis() - start

        Log.d("ZipUtils", "Descompactação concluída em ${tempo}ms")
        Log.d("ZipUtils", "Arquivos descompactados: $arquivos")
    }
}