package com.bfr.helloworld;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import android.util.Log;

/**
 * Clase que gestiona las estadísticas de las adivinanzas.
 * Se encarga de cargar, actualizar y guardar la información de aciertos y fallos en un archivo local (stats.json).
 */
public class StatsManager {
    private static final String FILE_NAME = "stats.json"; // Nombre del archivo donde se guardarán las estadísticas
    private Map<String, RiddleStats> statsMap; // Mapa que almacena las estadísticas de cada adivinanza
    private Context context; // Contexto de la aplicación, necesario para acceder al almacenamiento interno

    /**
     * Clase interna que representa las estadísticas de una adivinanza.
     * Guarda cuántas veces ha sido acertada (success) y cuántas veces ha sido fallada (failure).
     */
    public static class RiddleStats {
        public int success; // Número de aciertos de la adivinanza
        public int failure; // Número de fallos de la adivinanza

        // Constructor por defecto: inicia la estadística con 0 aciertos y 0 fallos.
        public RiddleStats() {
            this.success = 0;
            this.failure = 0;
        }

        // Constructor con valores iniciales específicos.
        public RiddleStats(int success, int failure) {
            this.success = success;
            this.failure = failure;
        }
    }

    /**
     * Constructor de StatsManager.
     * Se inicializa el mapa de estadísticas y se cargan los datos desde el archivo JSON si existe.
     */
    public StatsManager(Context context) {
        this.context = context;
        statsMap = new HashMap<>();
        loadStats(); // Cargar estadísticas previas desde el archivo JSON
    }

    /**
     * Carga las estadísticas desde el fichero JSON y las almacena en el mapa.
     */
    private void loadStats() {
        try {
            // Abre el archivo stats.json en modo lectura
            FileInputStream fis = context.openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;

            // Lee el archivo línea por línea y lo almacena en un StringBuilder
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Cierra los flujos de lectura
            reader.close();
            isr.close();
            fis.close();

            // Convierte el contenido del archivo en un objeto JSON
            String jsonString = sb.toString();

            JSONObject jsonObject = new JSONObject(jsonString);

            // Recorre todas las claves (adivinanzas) en el JSON
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next(); // Obtiene el nombre de la adivinanza (ej: "vaca")
                JSONObject statObj = jsonObject.getJSONObject(key);

                // Extrae los valores de aciertos y fallos
                int success = statObj.getInt("success");
                int failure = statObj.getInt("failure");

                // Guarda los valores en el mapa de estadísticas
                statsMap.put(key, new RiddleStats(success, failure));
            }
        } catch (Exception e) {
            // Si ocurre un error (ejemplo: el archivo no existe aún), se deja el mapa vacío.
            statsMap = new HashMap<>();
        }
    }

    /**
     * Guarda las estadísticas actuales en un archivo JSON.
     */
    private void saveStats() {
        try {
            JSONObject jsonObject = new JSONObject();

            // Recorre el mapa de estadísticas y convierte cada entrada en JSON
            for (Map.Entry<String, RiddleStats> entry : statsMap.entrySet()) {
                JSONObject statObj = new JSONObject();
                statObj.put("success", entry.getValue().success); // Añade número de aciertos
                statObj.put("failure", entry.getValue().failure); // Añade número de fallos
                jsonObject.put(entry.getKey(), statObj); // Almacena en el JSON con la clave de la adivinanza
            }

            // Convierte el objeto JSON a String y lo guarda en el archivo
            String jsonString = jsonObject.toString();

            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(jsonString.getBytes()); // Escribe el contenido en el archivo
            fos.close(); // Cierra el flujo de escritura
        } catch (Exception e) {
            e.printStackTrace(); // Muestra el error en la consola en caso de fallo
        }
    }

    /**
     * Actualiza las estadísticas de una adivinanza específica y guarda los cambios.
     * @param riddleKey Clave de la adivinanza (ejemplo: "vaca", "platano").
     * @param isSuccess Indica si la respuesta fue correcta (true) o incorrecta (false).
     */
    public void updateStats(String riddleKey, boolean isSuccess) {
        // Obtiene las estadísticas actuales de la adivinanza, o crea una nueva si no existe aún
        RiddleStats stats = statsMap.get(riddleKey);
        if (stats == null) {
            stats = new RiddleStats();
        }

        // Incrementa el contador de aciertos o fallos según corresponda
        if (isSuccess) {
            stats.success++;
        } else {
            stats.failure++;
        }

        // Guarda las estadísticas actualizadas en el mapa
        statsMap.put(riddleKey, stats);

        // Guarda los cambios en el archivo JSON
        saveStats();
    }

    /**
     * Devuelve el mapa con las estadísticas de todas las adivinanzas.
     * @return Mapa con claves de adivinanzas y sus estadísticas correspondientes.
     */
    public Map<String, RiddleStats> getStatsMap() {
        return statsMap;
    }
}
