package com.bfr.helloworld;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.speech.shared.ITTSCallback;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.List;
import java.util.Random;

public class AdivinanzasMedias {

    private MainActivity activity;
    private List<String> clavesAdivinanzas;

    public AdivinanzasMedias(MainActivity activity) {
        this.activity = activity;
        this.clavesAdivinanzas = FicheroAdivinanzas.respuestasMedias;
    }

    public void eliminarAdivinanza(String key) {
        clavesAdivinanzas.remove(key);
    }

    public void iniciarAdivinanza() {
        if (clavesAdivinanzas.isEmpty()) {
            BuddySDK.Speech.startSpeaking("¡Ya has completado todas las adivinanzas de nivel medio!");
            return;
        }

        Random random = new Random();
        int indice = random.nextInt(clavesAdivinanzas.size());
        String clave = clavesAdivinanzas.get(indice);

        FicheroAdivinanzas.AdivinanzaTexto adivinanza = FicheroAdivinanzas.adivinanzasMedias.get(clave);
        if (adivinanza == null) return;

        BuddySDK.Speech.startSpeaking(activity.obtenerFraseIntroduccionPersonalizada(), new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                BuddySDK.Speech.startSpeaking(adivinanza.pregunta, new ITTSCallback.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        activity.runOnUiThread(() -> {
                            activity.setContentView(R.layout.activity_main);
                            activity.opcionesContainer = activity.findViewById(R.id.opciones_container);
                            activity.opcionesContainer.setVisibility(View.VISIBLE);
                            Animation fadeIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
                            activity.opcionesContainer.startAnimation(fadeIn);

                            Log.d("ADIVINANZA_FLOW", "Se muestra la adivinanza: " + clave);
                            activity.setAdivinanzaKey(clave);
                            activity.setPreguntaActual(adivinanza.pregunta);

                            String[] opciones = adivinanza.opciones.toArray(new String[0]);
                            activity.configurarBotones(opciones, adivinanza.respuestaCorrecta);
                            activity.setPrimerIntento(true);
                        });
                    }

                    @Override public void onError(String error) throws RemoteException {
                        BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
                    }

                    @Override public void onPause() throws RemoteException {}
                    @Override public void onResume() throws RemoteException {}
                });
            }

            @Override public void onError(String error) throws RemoteException {
                BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
            }

            @Override public void onPause() throws RemoteException {}
            @Override public void onResume() throws RemoteException {}
        });
    }
}
