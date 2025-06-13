package com.bfr.helloworld;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddy.speech.shared.ITTSCallback;

import java.util.List;
import java.util.Random;
import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;



public class AdivinanzasBasicas {

    private MainActivity activity;
    private Context context;

    private List<String> clavesAdivinanzas;

    public AdivinanzasBasicas(MainActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        // Se cargan las claves desde el JSON personalizado cargado previamente
        this.clavesAdivinanzas = FicheroAdivinanzas.respuestasBasicas;
    }

    public void eliminarAdivinanza(String key) {
        clavesAdivinanzas.remove(key);
    }

    public void iniciarAdivinanza() {
        if (clavesAdivinanzas.isEmpty()) {
            BuddySDK.Speech.startSpeaking("¡Ya has completado todas las adivinanzas de nivel basico!");
            return;
        }

        Random random = new Random();
        int indice = random.nextInt(clavesAdivinanzas.size());
        String clave = clavesAdivinanzas.get(indice);

        FicheroAdivinanzas.AdivinanzaBasica adivinanza = FicheroAdivinanzas.adivinanzasBasicas.get(clave);
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
                            Animation zoomIn = AnimationUtils.loadAnimation(activity, R.anim.zoom_in_center);
                            activity.opcionesContainer.startAnimation(zoomIn);

                            Log.d("ADIVINANZA_FLOW", "Se muestra la adivinanza: " + clave);
                            activity.setAdivinanzaKey(clave);
                            activity.setPreguntaActual(adivinanza.pregunta);

                            String[] opciones = adivinanza.opciones.toArray(new String[0]);
                            String[] rutasImagenes = adivinanza.imagenes.toArray(new String[0]);
                            activity.configurarBotones(opciones, rutasImagenes, adivinanza.respuestaCorrecta);
                            activity.setPrimerIntento(true);
                        });
                    }
                    @Override public void onError(String error) throws RemoteException { BuddySDK.Speech.startSpeaking("Hubo un problema al hablar."); }
                    @Override public void onPause() throws RemoteException {}
                    @Override public void onResume() throws RemoteException {}
                });
            }
            @Override public void onError(String error) throws RemoteException { BuddySDK.Speech.startSpeaking("Hubo un problema al hablar."); }
            @Override public void onPause() throws RemoteException {}
            @Override public void onResume() throws RemoteException {}
        });
    }
}
