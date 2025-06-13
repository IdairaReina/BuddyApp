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

public class AdivinanzasDificiles {

    private MainActivity activity;
    private List<String> clavesAdivinanzas;

    public AdivinanzasDificiles(MainActivity activity) {
        this.activity = activity;
        this.clavesAdivinanzas = FicheroAdivinanzas.respuestasDificiles;
    }

    public void eliminarAdivinanza(String key) {
        clavesAdivinanzas.remove(key);
    }

    public void iniciarAdivinanza() {
        if (clavesAdivinanzas.isEmpty()) {
            BuddySDK.Speech.startSpeaking("¡Ya has completado todas las adivinanzas de nivel difícil!", new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    activity.runOnUiThread(() -> activity.mostrarPantallaResultados());
                }

                @Override public void onError(String error) throws RemoteException {}
                @Override public void onPause() throws RemoteException {}
                @Override public void onResume() throws RemoteException {}
            });
            return;
        }

        Random random = new Random();
        int indice = random.nextInt(clavesAdivinanzas.size());
        String clave = clavesAdivinanzas.get(indice);

        FicheroAdivinanzas.AdivinanzaTexto adivinanza = FicheroAdivinanzas.adivinanzasDificiles.get(clave);
        if (adivinanza == null) return;

        BuddySDK.Speech.startSpeaking(activity.obtenerFraseIntroduccionPersonalizada(), new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                BuddySDK.Speech.startSpeaking(adivinanza.pregunta, new ITTSCallback.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        activity.runOnUiThread(() -> {
                            activity.setContentView(R.layout.nivel_dificil_teclado);
                            activity.respuestasContainer = activity.findViewById(R.id.respuestas_container);
                            activity.respuestasContainer.setVisibility(View.VISIBLE);
                            Animation zoomIn = AnimationUtils.loadAnimation(activity, R.anim.zoom_in_center);
                            activity.opcionesContainer.startAnimation(zoomIn);

                            Log.d("ADIVINANZA_FLOW", "Se muestra la adivinanza: " + clave);
                            activity.setAdivinanzaKey(clave);
                            activity.setPreguntaActual(adivinanza.pregunta);

                            // Aceptamos respuestas con y sin artículo, para mejorar UX
                            String respuesta = adivinanza.respuestaCorrecta.trim().toLowerCase();
                            String[] respuestasValidas = {
                                    respuesta,
                                    "el " + respuesta,
                                    "es " + respuesta,
                                    "es el " + respuesta
                            };

                            activity.mostrarInterfazRespuestaDificil(respuestasValidas);
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
