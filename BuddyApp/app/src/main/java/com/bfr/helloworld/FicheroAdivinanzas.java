package com.bfr.helloworld;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class FicheroAdivinanzas {

    public static List<String> respuestasBasicas = new ArrayList<>();
    public static List<String> respuestasMedias = new ArrayList<>();
    public static List<String> respuestasDificiles = new ArrayList<>();

    public static Map<String, AdivinanzaBasica> adivinanzasBasicas = new HashMap<>();
    public static Map<String, AdivinanzaTexto> adivinanzasMedias = new HashMap<>();
    public static Map<String, AdivinanzaTexto> adivinanzasDificiles = new HashMap<>();

    private static String nombreFicheroSeleccionado = "adivinanzas_completo.json";

    public static void setNombreFichero(String nombre) {
        nombreFicheroSeleccionado = nombre;
    }


    public static void cargarAdivinanzas(Context context) {
        try {
            File file = new File(context.getFilesDir(), nombreFicheroSeleccionado);
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer);
            JSONObject root = new JSONObject(jsonStr);
            JSONArray adivinanzas = root.getJSONArray("adivinanzas");

            for (int i = 0; i < adivinanzas.length(); i++) {
                JSONObject obj = adivinanzas.getJSONObject(i);
                String nivel = obj.getString("nivel");
                String pregunta = obj.getString("pregunta");
                String correcta = obj.getString("respuestaCorrecta");

                List<String> textos = new ArrayList<>();
                List<String> imagenes = new ArrayList<>();

                if (obj.has("opciones")) {
                    JSONArray opciones = obj.getJSONArray("opciones");

                    for (int j = 0; j < opciones.length(); j++) {
                        JSONObject opcion = opciones.getJSONObject(j);

                        if (opcion.has("texto")) {
                            textos.add(opcion.getString("texto"));
                        }

                        if (nivel.equals("basico") && opcion.has("imagen")) {
                            imagenes.add(opcion.getString("imagen"));
                        }
                    }
                }

                switch (nivel) {
                    case "basico":
                        respuestasBasicas.add(correcta);
                        adivinanzasBasicas.put(correcta, new AdivinanzaBasica(pregunta, textos, imagenes, correcta));
                        break;
                    case "medio":
                        respuestasMedias.add(correcta);
                        adivinanzasMedias.put(correcta, new AdivinanzaTexto(pregunta, textos, correcta));
                        break;
                    case "dificil":
                        respuestasDificiles.add(correcta);
                        adivinanzasDificiles.put(correcta, new AdivinanzaTexto(pregunta, textos, correcta));
                        break;
                }
            }

        } catch (Exception e) {
            Log.e("FicheroAdivinanzas", "Error al cargar el JSON: " + e.getMessage());
        }
    }

    // Para nivel básico (con imágenes)
    public static class AdivinanzaBasica {
        public String pregunta;
        public List<String> opciones;
        public List<String> imagenes;
        public String respuestaCorrecta;

        public AdivinanzaBasica(String pregunta, List<String> opciones, List<String> imagenes, String correcta) {
            this.pregunta = pregunta;
            this.opciones = opciones;
            this.imagenes = imagenes;
            this.respuestaCorrecta = correcta;
        }
    }

    // Para nivel medio y difícil (solo texto)
    public static class AdivinanzaTexto {
        public String pregunta;
        public List<String> opciones;
        public String respuestaCorrecta;

        public AdivinanzaTexto(String pregunta, List<String> opciones, String correcta) {
            this.pregunta = pregunta;
            this.opciones = opciones;
            this.respuestaCorrecta = correcta;
        }
    }
}