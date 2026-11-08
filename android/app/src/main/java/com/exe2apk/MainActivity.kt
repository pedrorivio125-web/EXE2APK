
package com.exe2apk

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class MainActivity : Activity() {

    private lateinit var fileName: TextView
    private lateinit var status: TextView

    companion object {
        private const val PICK_EXE_FILE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(18, 18, 18))
        }

        val title = TextView(this).apply {
            text = "EXE2APK"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Prepare seus programas Windows para Android"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Selecionar arquivo EXE"
            setOnClickListener { openFilePicker() }
        }

        fileName = TextView(this).apply {
            text = "Nenhum arquivo selecionado"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        status = TextView(this).apply {
            text = "Aguardando um arquivo EXE."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(button)
        layout.addView(fileName)
        layout.addView(status)

        setContentView(layout)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_EXE_FILE)
    }

    @Deprecated("Using activity result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_EXE_FILE || resultCode != RESULT_OK) return

        val uri = data?.data ?: return
        val name = getFileName(uri)

        if (!name.lowercase().endsWith(".exe")) {
            fileName.text = "Arquivo inválido:\n$name"
            status.text = "Selecione um arquivo com extensão .exe."
            return
        }

        fileName.text = "Arquivo selecionado:\n$name"
        status.text = "Copiando e verificando o executável..."

        try {
            val savedFile = copyExeToPrivateStorage(uri, name)
            val architecture = detectPeArchitecture(savedFile)

            if (architecture == null) {
                savedFile.delete()
                status.text = "O arquivo não parece ser um EXE Windows válido (formato PE)."
                Toast.makeText(this, "EXE inválido ou não suportado.", Toast.LENGTH_LONG).show()
                return
            }

            fileName.text = "EXE preparado:\n$name"
            status.text = "Validado: Windows $architecture\nSalvo no EXE2APK.\n\nA execução ainda depende da integração do ambiente Wine/Box64."
            Toast.makeText(this, "EXE copiado e analisado!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            status.text = "Não foi possível verificar: ${e.message ?: "erro desconhecido"}"
            Toast.makeText(this, "Falha ao preparar o EXE.", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyExeToPrivateStorage(uri: Uri, originalName: String): File {
        val folder = File(filesDir, "executables")
        if (!folder.exists() && !folder.mkdirs()) {
            throw IllegalStateException("Não foi possível criar a pasta interna.")
        }

        val safeName = originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(folder, safeName)

        val input = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("O Android não conseguiu abrir o arquivo.")

        input.use { source ->
            FileOutputStream(destination).use { output ->
                source.copyTo(output)
                output.fd.sync()
            }
        }

        if (destination.length() == 0L) {
            destination.delete()
            throw IllegalStateException("O arquivo está vazio.")
        }

        return destination
    }

    private fun detectPeArchitecture(file: File): String? {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 64) return null

            if (raf.readUnsignedByte() != 0x4D ||
                raf.readUnsignedByte() != 0x5A
            ) return null

            raf.seek(0x3C)
            val b0 = raf.readUnsignedByte().toLong()
            val b1 = raf.readUnsignedByte().toLong()
            val b2 = raf.readUnsignedByte().toLong()
            val b3 = raf.readUnsignedByte().toLong()
            val peOffset = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)

            if (peOffset < 64 || peOffset + 6 > raf.length()) return null

            raf.seek(peOffset)
            if (raf.readUnsignedByte() != 0x50 ||
                raf.readUnsignedByte() != 0x45 ||
                raf.readUnsignedByte() != 0 ||
                raf.readUnsignedByte() != 0
            ) return null

            val machine = raf.readUnsignedByte() or
                (raf.readUnsignedByte() shl 8)

            return when (machine) {
                0x014C -> "x86 (32 bits)"
                0x8664 -> "x64 (64 bits)"
                0xAA64 -> "ARM64"
                else -> "arquitetura desconhecida (0x${machine.toString(16)})"
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "arquivo.exe"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index) ?: name
                }
            }
        }

        return name
    }
}

