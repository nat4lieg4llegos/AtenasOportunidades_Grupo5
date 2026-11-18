package cl.duoc.gestionoportunidades.nativo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FotoHelper {
    suspend fun guardar(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
            val archivo = File(carpeta, "evento_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            archivo.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cargar(ruta: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (ruta == null || !File(ruta).exists()) null else BitmapFactory.decodeFile(ruta)
    }

    fun descartar(ruta: String?) {
        ruta?.let { File(it).delete() }
    }
}
