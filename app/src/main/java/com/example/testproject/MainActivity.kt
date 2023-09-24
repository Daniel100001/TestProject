package com.example.testproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.testproject.databinding.ActivityMainBinding
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfFileUri: Uri

    companion object {
        const val STORAGE_CODE = 1001
        const val PDF_FILE_NAME = "MyPDF.pdf"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_CODE
                    )
                } else {
                    generateAndSavePDF()
                }
            } else {
                generateAndSavePDF()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generateAndSavePDF() {
        val document = Document()
        val directory =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString()
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val pdfFile = File(directory, PDF_FILE_NAME)

        try {
            val outputStream = FileOutputStream(pdfFile)
            document.pageSize = com.itextpdf.text.PageSize.A4
            PdfWriter.getInstance(document, outputStream)
            document.open()

            // Create a Bitmap with the circle
            val diameterCm = 10f
            val diameterPt = diameterCm * 28.3465f // Convert cm to points
            val radiusPt = diameterPt / 2f
            val bitmap = Bitmap.createBitmap(
                diameterPt.toInt(),
                diameterPt.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = Color.BLACK
            canvas.drawCircle(radiusPt, radiusPt, radiusPt, paint)

            // Add the Bitmap to the PDF
            val image = Image.getInstance(bitmapToByteArray(bitmap))
            document.add(image)

            document.close()
            outputStream.close()

            pdfFileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )

            Toast.makeText(this, "PDF saved to ${pdfFile.absolutePath}", Toast.LENGTH_SHORT)
                .show()

            // Optionally, you can send the PDF via Intent
            sendPdfViaIntent(pdfFileUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: {$e}", Toast.LENGTH_SHORT).show()
            Log.e("error", e.toString())
        }
    }


    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun sendPdfViaIntent(pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/pdf"
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri)
        startActivity(Intent.createChooser(shareIntent, "Share PDF using"))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    generateAndSavePDF()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
