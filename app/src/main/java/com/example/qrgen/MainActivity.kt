package com.example.qrgen

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class MainActivity : Activity() {

    private var qrBitmap: Bitmap? = null
    private lateinit var input: EditText
    private lateinit var qrImage: ImageView
    private lateinit var saveButton: Button

    private fun px(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(48), px(16), px(16))
        }

        // Поле вводу URL
        input = EditText(this).apply {
            hint = "https://example.com"
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                            event.action == KeyEvent.ACTION_DOWN)
                ) {
                    generateQr()
                    true
                } else false
            }
        }
        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Кнопка Enter
        val enterButton = Button(this).apply {
            text = "Enter"
            setOnClickListener { generateQr() }
        }
        root.addView(
            enterButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // QR-код (спочатку прихований)
        qrImage = ImageView(this).apply { visibility = View.GONE }
        root.addView(
            qrImage,
            LinearLayout.LayoutParams(px(300), px(300)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = px(24)
            }
        )

        // Кнопка "Зберегти в галерею" (спочатку прихована)
        saveButton = Button(this).apply {
            text = "Зберегти в галерею"
            visibility = View.GONE
            setOnClickListener { saveToGallery() }
        }
        root.addView(
            saveButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = px(16)
            }
        )

        setContentView(root)
    }

    private fun generateQr() {
        var text = input.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Введіть URL", Toast.LENGTH_SHORT).show()
            return
        }
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            text = "https://$text"
        }

        try {
            val size = 1000
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val pixels = IntArray(size * size) { i ->
                if (matrix.get(i % size, i / size)) Color.BLACK else Color.WHITE
            }
            val bmp = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            qrBitmap = bmp
            qrImage.setImageBitmap(bmp)
            qrImage.visibility = View.VISIBLE
            saveButton.visibility = View.VISIBLE

            // Сховати клавіатуру
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveToGallery() {
        val bmp = qrBitmap ?: return
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/QRCodes"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                Toast.makeText(this, "Не вдалося зберегти", Toast.LENGTH_SHORT).show()
                return
            }
            contentResolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)

            Toast.makeText(this, "Збережено в галерею (Pictures/QRCodes)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
