package aykuttasil.com.playgroundsmsretriever

import android.app.Application

/**
 * Created by aykutasil on 13.11.2017.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            /*Following will generate the hash code*/
            val appSignature = AppSignatureHelper(this)
            appSignature.appSignatures
        }
    }

}