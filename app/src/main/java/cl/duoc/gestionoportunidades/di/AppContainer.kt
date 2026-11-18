package cl.duoc.gestionoportunidades.di

import android.content.Context
import cl.duoc.gestionoportunidades.data.local.AtenasDbHelper
import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.local.SesionManager
import cl.duoc.gestionoportunidades.data.remote.RetrofitInstance
import cl.duoc.gestionoportunidades.repository.AuthRepository
import cl.duoc.gestionoportunidades.repository.FeriadoRepository
import cl.duoc.gestionoportunidades.repository.OportunidadRepository

object AppContainer {
    @Volatile private var oportunidadRepo: OportunidadRepository? = null
    @Volatile private var authRepo: AuthRepository? = null
    @Volatile private var feriadoRepo: FeriadoRepository? = null

    fun oportunidades(context: Context): OportunidadRepository =
        oportunidadRepo ?: synchronized(this) {
            oportunidadRepo ?: AtenasDbHelper(context.applicationContext).let { helper ->
                OportunidadRepository(OportunidadDao(helper), SeguimientoDao(helper), RetrofitInstance.api)
            }.also { oportunidadRepo = it }
        }

    fun auth(context: Context): AuthRepository =
        authRepo ?: synchronized(this) {
            authRepo ?: AuthRepository(SesionManager(context.applicationContext)).also { authRepo = it }
        }

    fun feriados(): FeriadoRepository =
        feriadoRepo ?: synchronized(this) {
            feriadoRepo ?: FeriadoRepository(RetrofitInstance.feriadosApi).also { feriadoRepo = it }
        }
}
