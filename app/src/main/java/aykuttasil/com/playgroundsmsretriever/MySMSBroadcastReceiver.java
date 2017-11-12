package aykuttasil.com.playgroundsmsretriever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * Created by aykutasil on 12.11.2017.
 */

public class MySMSBroadcastReceiver extends BroadcastReceiver {
    private OTPReceiveListener otpReceiver;

    public void initOTPListener(OTPReceiveListener receiver) {
        this.otpReceiver = receiver;
    }


    /**
     * SMS geldiğinde buraya düşer.
     * Gelen SMS '<#>' ile başlamalıdır.
     * https://developers.google.com/identity/sms-retriever/verify
     *
     * Gelen sms in yakalanabilmesi için sms içeriğinde uygulamanın hash kodunun ilk 11 hanesi bulunmalıdır.
     *
     * Örnek SMS
     * <#> Uygulama Kodu: 123ABC78
     * FA+9qCX9VSu
     *
     * FA+9qCX9VSu -> Uygulamanın keystore u hashlenerek ilk 11 hanesi alınmıştır.
     * Hash stringi elde etmek için {{@link AppSignatureHelper}} kullanılabilir.
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch (status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    // SMS metnini alıyoruz
                    String otp = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);

                    // Extract one-time code from the message and complete verification
                    // by sending the code back to your server for SMS authenticity.
                    // But here we are just passing it to MainActivity
                    // SMS içeriğini filtreleyerek mevcut kodu alıyoruz
                    if (otpReceiver != null) {
                        otp = otp.replace("<#>  Uygulama Kodunuz: ", "").split("\n")[0];
                        otpReceiver.onOTPReceived(otp);
                    }
                    break;

                case CommonStatusCodes.TIMEOUT:
                    // Waiting for SMS timed out (5 minutes)
                    // Handle the error ...
                    if (otpReceiver != null) {
                        otpReceiver.onOTPTimeOut();
                    }
                    break;
            }
        }
    }

    public interface OTPReceiveListener {

        void onOTPReceived(String otp);

        void onOTPTimeOut();
    }
}