
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

class MainActivity : Activity() {

    private lateinit var fileName: TextView

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
            text = "Windows games on Android"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Selecionar arquivo EXE"
            setOnClickListener {
                openFilePicker()
            }
        }

        fileName = TextView(this).apply {
            text = "Nenhum arquivo selecionado"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(button)
        layout.addView(fileName)

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

        if (requestCode == PICK_EXE_FILE &&
            resultCode == RESULT_OK
        ) {
            val uri: Uri = data?.data ?: return

            val name = getFileName(uri)

            if (name.lowercase().endsWith(".exe")) {
                fileName.text = "EXE selecionado:\n$name"

                Toast.makeText(
                    this,
                    "Arquivo selecionado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                fileName.text = "Arquivo inválido:\n$name"

                Toast.makeText(
                    this,
                    "Escolha um arquivo com extensão .exe",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "Arquivo desconhecido"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name
    }
}
