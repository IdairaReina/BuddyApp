package com.bfr.helloworld;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

public class ServidorHTTP extends NanoHTTPD {

    private static final String TAG = "ServidorHTTP";
    private final Context context;

    public ServidorHTTP(int port, Context context) {
        super(port);
        this.context = context;
    }

    private Runnable callbackAlRecibirFormulario;

    public void setCallbackAlRecibirFormulario(Runnable callback) {
        this.callbackAlRecibirFormulario = callback;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Petición: " + session.getUri() + " | Método: " + session.getMethod());

        if (Method.GET.equals(session.getMethod()) && session.getUri().equals("/")) {
            return servirFormulario();
        } else if (Method.POST.equals(session.getMethod()) && session.getUri().equals("/submit")) {
            return procesarFormulario(session);
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Ruta no encontrada.");
        }
    }

    private Response servirFormulario() {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("formulario_profesor/index.html");
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String htmlContent = scanner.hasNext() ? scanner.next() : "";
            return newFixedLengthResponse(Response.Status.OK, "text/html", htmlContent);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error cargando el formulario.");
        }
    }

    private Response procesarFormulario(IHTTPSession session) {
        JSONObject resultadoFinal = new JSONObject();
        JSONArray adivinanzas = new JSONArray();

        try {
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();

            String nombreArchivo = params.get("nombre_archivo");
            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                nombreArchivo = "adivinanzas_generico";
            }

            nombreArchivo = nombreArchivo.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".json";

            File imagesDir = new File(context.getFilesDir(), "imagenes_basico");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            int maxIndex = 50;
            for (int i = 0; i < maxIndex; i++) {
                String pb = params.get("pregunta_basico_" + i);
                if (pb != null && !pb.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    obj.put("nivel", "basico");
                    obj.put("pregunta", pb);
                    obj.put("respuestaCorrecta", params.get("correcta_basico_" + i));

                    JSONArray opciones = new JSONArray();
                    for (int j = 1; j <= 5; j++) {
                        JSONObject opcion = new JSONObject();
                        String texto = params.get("opcion" + j + "_basico_" + i);
                        String imagenKeyEsperada = "imagen" + j + "_basico_" + i;
                        String imagenPath = null;

                        for (String key : files.keySet()) {
                            if (key.startsWith(imagenKeyEsperada)) {
                                imagenPath = files.get(key);
                                break;
                            }
                        }

                        String rutaFinal = "";
                        if (imagenPath != null && texto != null && !texto.trim().isEmpty()) {
                            File origen = new File(imagenPath);

                            // Generar nombre único basado en el texto de la opción
                            String nombreOriginal = texto.replaceAll("\\s+", "_") + "_" + j + "_basico_" + i + "_" + System.currentTimeMillis() + ".png";
                            File destino = new File(imagesDir, nombreOriginal);

                            try (FileInputStream fis = new FileInputStream(origen);
                                 FileOutputStream fos = new FileOutputStream(destino)) {
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, length);
                                }
                                rutaFinal = destino.getAbsolutePath();
                            }
                        }

                        opcion.put("texto", texto);
                        opcion.put("imagen", rutaFinal);
                        opciones.put(opcion);
                    }

                    obj.put("opciones", opciones);
                    adivinanzas.put(obj);
                }

                String pm = params.get("pregunta_medio_" + i);
                if (pm != null && !pm.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    obj.put("nivel", "medio");
                    obj.put("pregunta", pm);
                    obj.put("respuestaCorrecta", params.get("correcta_medio_" + i));

                    JSONArray opciones = new JSONArray();
                    for (int j = 1; j <= 5; j++) {
                        JSONObject opcion = new JSONObject();
                        opcion.put("texto", params.get("opcion" + j + "_medio_" + i));
                        opciones.put(opcion);
                    }

                    obj.put("opciones", opciones);
                    adivinanzas.put(obj);
                }

                String pd = params.get("pregunta_dificil_" + i);
                if (pd != null && !pd.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    obj.put("nivel", "dificil");
                    obj.put("pregunta", pd);
                    obj.put("respuestaCorrecta", params.get("correcta_dificil_" + i));
                    obj.put("opciones", new JSONArray());
                    adivinanzas.put(obj);
                }
            }

            resultadoFinal.put("adivinanzas", adivinanzas);
            File outputFile = new File(context.getFilesDir(), nombreArchivo);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(resultadoFinal.toString(4).getBytes());
                fos.flush();
            }

        } catch (IOException | JSONException | ResponseException e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error al procesar el formulario.");
        }

        // Notificar a la actividad que se recibió el formulario
        if (callbackAlRecibirFormulario != null) {
            callbackAlRecibirFormulario.run();
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_HTML,
                "<html><body><h3>¡Adivinanzas guardadas correctamente!</h3><a href='/'>Volver al formulario</a></body></html>");
    }
}