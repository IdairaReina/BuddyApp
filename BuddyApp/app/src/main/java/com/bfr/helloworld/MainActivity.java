package com.bfr.helloworld;

import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import java.io.File;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.util.Random;


import com.bfr.buddy.speech.shared.ISTTCallback;
import com.bfr.buddy.speech.shared.ITTSCallback;
import com.bfr.buddy.speech.shared.STTResultsData;
import com.bfr.buddy.ui.shared.FaceTouchData;
import com.bfr.buddy.ui.shared.FacialEvent;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.IUIFaceTouchCallback;
import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;
import com.bfr.buddysdk.services.speech.STTTask;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import android.widget.ImageButton;




import java.util.Locale;

import android.media.MediaPlayer;

public class MainActivity extends BuddyActivity {

    // ---------------------------------------------------------------------------------------------
    // 1) ENUM PARA CONTROLAR EL ESTADO DE LA CONVERSACIÓN
    // ---------------------------------------------------------------------------------------------
    private enum EstadoConversacion {
        PREGUNTANDO_SI_JUGAR,
        PREGUNTANDO_NOMBRE,
        JUGANDO_BASICO,
        JUGANDO_MEDIO,
        JUGANDO_DIFICIL
    }

    private EstadoConversacion estadoActual;

    // ---------------------------------------------------------------------------------------------
    // VARIABLES NORMALES
    // ---------------------------------------------------------------------------------------------
    private STTTask sttTask;
    private int puntuacionTotal = 0;
    private boolean primerIntento = true;
    private int numeroIntentos = 0;
    private int aciertosTotales = 0;
    private int fallosTotales = 0;

    private TextView btnPuntuacion;
    private ImageButton btnRepetir;
    private String preguntaActual;

    // Mantenemos una única instancia para cada clase de adivinanzas
    private AdivinanzasBasicas adivinanzasBasicas;
    private AdivinanzasMedias adivinanzasMedias;
    private AdivinanzasDificiles adivinanzasDificiles;

    private String nivelAnterior = "basico";
    //private StatsManager statsManager;  // Instancia de StatsManager
    private String adivinanzaKey;       // clave de la adivinanza actual

    public View opcionesContainer;

    public View layoutResultados;

    public View rolInicio;

    public View respuestasContainer;
    private String nombreUsuario = "";
    private boolean estaEscuchandoRespuesta = false;
    private boolean movimientoEnProgreso = false;

    private MediaPlayer mediaPlayer;

    private MediaPlayer sonidoP2;

    private boolean esModoUsuario = false; // se activa solo si el usuario pulsa el botón de iniciar como usuario

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (sonidoP2 != null) {
            sonidoP2.release();
            sonidoP2 = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    public void reiniciarJuego() {
        puntuacionTotal = 0;
        aciertosTotales = 0;
        fallosTotales = 0;
        primerIntento = true;
        numeroIntentos = 0;
        nivelAnterior = "basico";
        nombreUsuario = "";
        estadoActual = EstadoConversacion.PREGUNTANDO_SI_JUGAR;
    }

    public String obtenerFraseIntroduccionPersonalizada() {
        String[] frases = {
                "¡Vamos con una adivinanza %s!",
                "A ver si aciertas esta %s.",
                "Prepárate, %s esta es buena.",
                "Veamos qué tan listo estás %s.",
                "%s Escucha bien...",
                "Adivina adivinanza %s...",
                "Concentración %s esta es complicada.",
                "Esta te la sabes %s.",
                "Vamos a ver si puedes con esta %s.",
                "¡Mucha suerte %s! Escucha con atención.",
                "A ver si esta la aciertas al primer intento %s.",
                "Esta es un clásico %s seguro la sabes.",
                "¡Atención %s! Se viene una buena.",
                "¿Listo %s? ¡Aquí va!",
                "¿Te atreves con esta %s?",
                "No bajes la guardia %s. Esta tiene truco.",
                "A ver qué tal esta %s.",
                "¡Hora de demostrar tu ingenio %s!"
        };

        int indice = new Random().nextInt(frases.length);
        return String.format(frases[indice], getNombreUsuario());
    }

    public String obtenerFraseErrorPersonalizada() {
        String[] frasesError = {
                "No es correcta %s. ¡La próxima seguro la aciertas!",
                "Casi lo logras %s. Inténtalo de nuevo.",
                "Esa no era %s, pero no te rindas.",
                "¡Ánimo %s! Seguro que la próxima es la tuya.",
                "No era esa sigue intentándolo %s",
                "Incorrecto esta vez %s",
                "Ups no es esa %s ¡Piensala mejor!",
                "Fallaste esta %s pero seguro que la siguiente es pan comido.",
                "No es correcto %s vamos a por otra oportunidad.",
                "Ay casi %s No te rindas.",
                "No ha sido esta vez %s pero vas mejorando.",
                "Esa no era %s, ¡pero estás muy cerca!",
                "Tranquilo %s, aún tienes otra oportunidad."
        };

        int indice = new Random().nextInt(frasesError.length);
        return String.format(frasesError[indice], getNombreUsuario());
    }


    private void copiarFicheroSiNoExisteConImagenes(String nombreFichero) {
        File destino = new File(getFilesDir(), nombreFichero);
        if (!destino.exists()) {
            try {
                InputStream is = getAssets().open(nombreFichero);
                StringBuilder builder = new StringBuilder();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    builder.append(new String(buffer, 0, length));
                }
                is.close();

                JSONObject json = new JSONObject(builder.toString());
                JSONArray adivinanzas = json.getJSONArray("adivinanzas");
                File imagenesDir = new File(getFilesDir(), "imagenes_basico");
                if (!imagenesDir.exists()) imagenesDir.mkdirs();

                for (int i = 0; i < adivinanzas.length(); i++) {
                    JSONObject adivinanza = adivinanzas.getJSONObject(i);
                    if (!adivinanza.getString("nivel").equals("basico")) continue;

                    JSONArray opciones = adivinanza.getJSONArray("opciones");
                    for (int j = 0; j < opciones.length(); j++) {
                        JSONObject opcion = opciones.getJSONObject(j);
                        if (opcion.has("imagen")) {
                            String rutaOriginal = opcion.getString("imagen");
                            String nombreImagen = rutaOriginal.substring(rutaOriginal.lastIndexOf("/") + 1);
                            InputStream imgStream;

                            try {
                                imgStream = getAssets().open("imagenes_basico/" + nombreImagen);
                            } catch (IOException e) {
                                Log.w("COPIA", "Imagen no encontrada en assets: " + nombreImagen);
                                continue;
                            }

                            File nuevaRuta = new File(imagenesDir, nombreImagen);
                            try (FileOutputStream fos = new FileOutputStream(nuevaRuta)) {
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = imgStream.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                                opcion.put("imagen", nuevaRuta.getAbsolutePath());
                            }

                            imgStream.close();
                        }
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(destino)) {
                    fos.write(json.toString(4).getBytes());
                    fos.flush();
                }

                Log.d("COPIA", "Archivo JSON y sus imágenes copiados correctamente.");

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Log.e("COPIA", "Error al copiar el fichero o procesar imágenes.");
            }
        }
    }

    private void borrarJsonLocal(String nombreFichero) {
        File destino = new File(getFilesDir(), nombreFichero);
        if (destino.exists()) {
            boolean eliminado = destino.delete();
            Log.d("BORRADO", "Archivo " + nombreFichero + (eliminado ? " eliminado correctamente." : " no pudo ser eliminado."));
        } else {
            Log.d("BORRADO", "El archivo " + nombreFichero + " no existe.");
        }
    }


    // ---------------------------------------------------------------------------------------------
    // onCreate
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        copiarFicheroSiNoExisteConImagenes("adivinanzas.json");
        copiarFicheroSiNoExisteConImagenes("trivial.json");

        // Inicializa el StatsManager
        //statsManager = new StatsManager(this);

        // Crea las instancias únicas para cada nivel
        adivinanzasBasicas = new AdivinanzasBasicas(this);
        adivinanzasMedias = new AdivinanzasMedias(this);
        adivinanzasDificiles = new AdivinanzasDificiles(this);

        // Mostramos primero el layout para elegir rol
        setContentView(R.layout.rol_de_inicio);
        rolInicio = findViewById(R.id.layout_inicio);
        mediaPlayer = MediaPlayer.create(this, R.raw.sonido_p1);
        // Configuramos el botón de usuario
        ImageButton btnUsuario = findViewById(R.id.btn_iniciar_usuario);
        btnUsuario.setOnClickListener(view -> {
            mediaPlayer.start();
            rolInicio.setVisibility(View.GONE);
            mostrarMenuSeleccionDeFichero();
        });

        // Configuramos el botón de profesor
        ImageButton btnProfesor = findViewById(R.id.btn_iniciar_profesor);
        btnProfesor.setOnClickListener(view -> {
            mediaPlayer.start();
            rolInicio.setVisibility(View.GONE);
            iniciarFlujoProfesor();
        });

    }

    public void mostrarPantallaResultados() {
        setContentView(R.layout.pantalla_resultados);
        layoutResultados = findViewById(R.id.layout_resultados);

        BuddySDK.Speech.startSpeaking("¡Has terminado la partida, " + getNombreUsuario() + "! Espero que jueguemos pronto de nuevo. Estos son tus resultados.");

        // Referencias
        TextView textoPuntuacion = findViewById(R.id.texto_puntuacion);
        TextView textoNivel = findViewById(R.id.texto_nivel);
        TextView textoEstadisticas = findViewById(R.id.texto_estadisticas);
        ImageButton btnVolverMenu = findViewById(R.id.btn_volver_menu);

        Animation zoomDesdeEsquina = AnimationUtils.loadAnimation(this, R.anim.zoom_in_bottom_right);
        layoutResultados.startAnimation(zoomDesdeEsquina);

        // Setear texto
        textoPuntuacion.setText("Has conseguido " + puntuacionTotal + " puntos. ¡Muy bien hecho " + getNombreUsuario() + "!");
        textoNivel.setText("Alcanzaste el nivel " + obtenerNivelActual());
        textoEstadisticas.setText("Número de aciertos: " + aciertosTotales + "\nNúmero de fallos: " + fallosTotales);

        // Botón de volver
        btnVolverMenu.setOnClickListener(view -> {
            reiniciarJuego();
            setContentView(R.layout.menu);
            mostrarMenuSeleccionDeFichero();
        });
    }



    private void iniciarFlujoUsuario() {
        esModoUsuario = true;
        FicheroAdivinanzas.cargarAdivinanzas(this);
        onSDKReady();
    }

    private void mostrarMenuSeleccionDeFichero() {
        setContentView(R.layout.menu);

        File dir = getFilesDir(); // Ruta: /data/data/com.bfr.helloworld/files/
        File[] archivos = dir.listFiles((dir1, name) -> name.endsWith(".json"));
        LinearLayout contenedor = findViewById(R.id.contenedor_ficheros);

        if (archivos != null && archivos.length > 0) {
            for (File archivo : archivos) {
                String nombre = archivo.getName();
                String nombreVisible = nombre.replace(".json", "").replace("_", " ");

                // Contenedor tipo tarjeta para cada botón
                LinearLayout layoutBoton = new LinearLayout(this);
                layoutBoton.setOrientation(LinearLayout.VERTICAL);
                layoutBoton.setPadding(30, 30, 30, 30);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(0, 20, 0, 20); // Separación entre rectángulos
                layoutBoton.setLayoutParams(layoutParams);
                layoutBoton.setBackgroundResource(R.drawable.rectangulo_fichero); // Fondo personalizado

                // Botón dentro del contenedor
                Button btn = new Button(this);
                btn.setText(nombreVisible);
                btn.setAllCaps(false);
                btn.setBackgroundColor(Color.TRANSPARENT); // Fondo transparente para que se vea el fondo del layout
                btn.setTextSize(18);
                btn.setPadding(0, 20, 0, 20);
                btn.setTextColor(Color.BLACK);
                btn.setTypeface(Typeface.DEFAULT_BOLD);

                sonidoP2 = MediaPlayer.create(this, R.raw.sonido_p2); // Fuera del bucle si quieres reutilizar

                btn.setOnClickListener(view -> {
                    if (sonidoP2.isPlaying()) {
                        sonidoP2.seekTo(0);
                    }
                    sonidoP2.start();

                    View layoutMenu = findViewById(R.id.layout_menu);
                    if (layoutMenu != null) {
                        layoutMenu.setVisibility(View.GONE);
                    }
                    FicheroAdivinanzas.setNombreFichero(nombre); // Asignamos el fichero
                    iniciarFlujoUsuario(); // Flujo normal
                });

                layoutBoton.addView(btn);
                contenedor.addView(layoutBoton);
            }
        } else {
            TextView aviso = new TextView(this);
            aviso.setText("No se encontraron archivos de adivinanzas.");
            aviso.setTextSize(18);
            contenedor.addView(aviso);
        }
    }

    private String obtenerIPLocal() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "localhost"; // Por si no encuentra ninguna
    }


    private void iniciarFlujoProfesor() {
        String ip = obtenerIPLocal(); // Obtener la IP real del dispositivo
        String url = "http://" + ip + ":8080";
        BuddySDK.UI.setMood(FacialExpression.HAPPY);

        // Buddy habla primero
        BuddySDK.Speech.startSpeaking("¡Has iniciado como profesor! Navega a este enlace y rellena el formulario con tus preguntas porfi", new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String iText) throws RemoteException {
                // Mostrar layout con URL cuando termine de hablar
                runOnUiThread(() -> {
                    setContentView(R.layout.url_nanoserver);
                    ImageButton btnMenu = findViewById(R.id.btn_menu_profesor);
                    btnMenu.setOnClickListener(v -> mostrarMenuSeleccionDeFichero());
                    TextView textUrl = findViewById(R.id.texto_url_profesor);
                    textUrl.setText(url);
                });
            }

            @Override
            public void onError(String error) throws RemoteException {
                Log.e("Buddy", "Error al hablar en modo profesor: " + error);
            }

            @Override public void onPause() throws RemoteException {}
            @Override public void onResume() throws RemoteException {}
        });

        // Arrancar servidor
        ServidorHTTP servidor = new ServidorHTTP(8080, getApplicationContext());
        try {
            servidor.start();
            Log.d("ServidorHTTP", "Servidor iniciado en " + url);
        } catch (IOException e) {
            Log.e("ServidorHTTP", "Error al iniciar el servidor: " + e.getMessage());
        }

        servidor.setCallbackAlRecibirFormulario(() -> {
            runOnUiThread(() -> {
                BuddySDK.Speech.startSpeaking(
                        "He recibido tu formulario. Pulsa el botón del menú para verlo, también puedes seguir enviándome formularios."
                );
            });
        });

    }


    // Método para asignar la clave de la adivinanza (por ejemplo, "vaca")
    public void setAdivinanzaKey(String key) {
        this.adivinanzaKey = key;
    }

    // ---------------------------------------------------------------------------------------------
    // onSDKReady
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onSDKReady() {
        if (!esModoUsuario) return; // No hacer nada si no ha pulsado el botón de usuario
        // Al arrancar, estamos preguntando si quiere jugar
        estadoActual = EstadoConversacion.PREGUNTANDO_SI_JUGAR;

        BuddySDK.Speech.startSpeaking("¿Quieres jugar a las adivinanzas conmigo?", new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String iText) throws RemoteException {
                escucharRespuesta();
            }

            @Override
            public void onError(String error) throws RemoteException {
                BuddySDK.Speech.startSpeaking("Hubo un error al hablar");
            }

            @Override
            public void onPause() throws RemoteException { }

            @Override
            public void onResume() throws RemoteException { }
        });

        BuddySDK.UI.addFaceTouchListener(new IUIFaceTouchCallback.Stub() {
            @Override
            public void onTouch(FaceTouchData faceTouchData) throws RemoteException {
                BuddySDK.UI.playFacialEvent(FacialEvent.SMILE);
                BuddySDK.UI.playFacialEvent(FacialEvent.WHISTLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    BuddySDK.UI.setMood(FacialExpression.NEUTRAL);
                }, 3000);
            }

            @Override
            public void onRelease(FaceTouchData faceTouchData) throws RemoteException { }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // MÉTODO PARA DETENER CUALQUIER STT ACTIVO
    // ---------------------------------------------------------------------------------------------
    private void detenerSTTActual() {
        if (sttTask != null) {
            sttTask.stop();
            sttTask = null;
            estaEscuchandoRespuesta = false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ESCUCHAR RESPUESTA - se usa cuando PREGUNTAMOS “¿QUIERES JUGAR?”
    // ---------------------------------------------------------------------------------------------
    private void escucharRespuesta() {
        // Detenemos STT previo (si lo hubiera)
        detenerSTTActual();

        // Arrancamos escucha
        estaEscuchandoRespuesta = true;
        sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
        sttTask.start(false, new ISTTCallback.Stub() {
            @Override
            public void onSuccess(STTResultsData resultsData) throws RemoteException {
                if (!estaEscuchandoRespuesta) return;

                estaEscuchandoRespuesta = false;
                if (resultsData.getResults() != null && !resultsData.getResults().isEmpty()) {
                    String respuesta = resultsData.getResults().get(0).getUtterance().toLowerCase();
                    String clasificacion = analizarRespuestaPropia(respuesta);
                    procesarRespuesta(clasificacion);
                }
            }

            @Override
            public void onError(String error) throws RemoteException {
                if (!estaEscuchandoRespuesta) return;
                estaEscuchandoRespuesta = false;
                BuddySDK.Speech.startSpeaking("No entendí tu respuesta");
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // PROCESAR RESPUESTA - usando el estadoActual para no mezclar flujos
    // ---------------------------------------------------------------------------------------------
    private void procesarRespuesta(String clasificacion) {
        switch (estadoActual) {
            case PREGUNTANDO_SI_JUGAR:
                // Solo aquí interpretamos "afirmativa"/"negativa" para jugar
                if (clasificacion.equals("afirmativa")) {
                    BuddySDK.Speech.startSpeaking("¡Genial! Vamos a jugar.", new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            runOnUiThread(() -> {
                                BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                Log.d("BuddyMove", "Llamando a MovimientoCelebracion()");
                                MovimientoCelebracion();
                                estaEscuchandoRespuesta = false;

                                // Cambiamos estado a PREGUNTANDO_NOMBRE
                                estadoActual = EstadoConversacion.PREGUNTANDO_NOMBRE;

                                // Tras celebrar, pedimos el nombre
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    escucharNombreUsuario();
                                }, 3500);
                            });
                        }

                        @Override
                        public void onError(String error) throws RemoteException {
                            BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
                        }

                        @Override
                        public void onPause() throws RemoteException { }

                        @Override
                        public void onResume() throws RemoteException { }
                    });

                } else if (clasificacion.equals("negativa")) {
                    estaEscuchandoRespuesta = false;
                    BuddySDK.Speech.startSpeaking("Oh, qué pena. Tal vez después entonces.");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        BuddySDK.UI.setMood(FacialExpression.SAD);
                    }, 1000);

                } else {
                    // Inconclusa
                    estaEscuchandoRespuesta = true;
                    BuddySDK.Speech.startSpeaking("No entendí. ¿Puedes repetir?");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        BuddySDK.UI.setMood(FacialExpression.THINKING);
                        escucharRespuesta();
                    }, 1000);
                }
                break;

            case PREGUNTANDO_NOMBRE:
                // Si llega aquí un "sí" o "no", realmente no aplica
                // porque estamos esperando el NOMBRE, no sí/no.
                BuddySDK.Speech.startSpeaking("Necesito que me digas tu nombre, no un sí o no.");
                break;

            case JUGANDO_BASICO:
            case JUGANDO_MEDIO:
            case JUGANDO_DIFICIL:
                // Si en medio del juego dice algo que el método "analizarRespuestaPropia"
                // identifica como "sí" o "no", lo ignoramos o le decimos que conteste la adivinanza
                BuddySDK.Speech.startSpeaking("Responde a la adivinanza, por favor.");
                break;

            default:
                // Cualquier otro caso
                BuddySDK.Speech.startSpeaking("No entendí.");
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ESCUCHAR NOMBRE
    // ---------------------------------------------------------------------------------------------
    private void escucharNombreUsuario() {
        // Estamos en estado PREGUNTANDO_NOMBRE
        // Detenemos STT activo
        detenerSTTActual();

        BuddySDK.Speech.startSpeaking("Pero, primero dime tu nombre, porfi", new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                // Iniciamos STT para el nombre
                detenerSTTActual();
                sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
                sttTask.start(false, new ISTTCallback.Stub() {
                    @Override
                    public void onSuccess(STTResultsData resultsData) throws RemoteException {
                        if (resultsData.getResults() != null && !resultsData.getResults().isEmpty()) {
                            String nombreEscuchado = resultsData.getResults().get(0).getUtterance();

                            nombreUsuario = nombreEscuchado;

                            BuddySDK.Speech.startSpeaking("Tu nombre es " + nombreEscuchado, new ITTSCallback.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    runOnUiThread(() -> {
                                        setContentView(R.layout.confirmar_nombre);

                                        TextView textoConfirmarNombre = findViewById(R.id.text_pregunta_nombre);
                                        textoConfirmarNombre.setText(nombreEscuchado + " ¿es correcto?");

                                        Button btnSi = findViewById(R.id.btn_si);
                                        Button btnNo = findViewById(R.id.btn_no);

                                        btnSi.setOnClickListener(view -> {
                                            View layoutConfirmacion = findViewById(R.id.layout_confirmacion);
                                            if (layoutConfirmacion != null) {
                                                layoutConfirmacion.setVisibility(View.GONE);
                                            }
                                            BuddySDK.Speech.startSpeaking("¡Encantado de conocerte " + nombreEscuchado + "!", new ITTSCallback.Stub() {
                                                @Override
                                                public void onSuccess(String s) throws RemoteException {
                                                    // Ahora iniciamos las adivinanzas
                                                    runOnUiThread(() -> iniciarAdivinanzaSegunNivel());
                                                    BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                                }
                                                @Override public void onError(String error) throws RemoteException {}
                                                @Override public void onPause() throws RemoteException {}
                                                @Override public void onResume() throws RemoteException {}
                                            });
                                        });

                                        btnNo.setOnClickListener(view -> {
                                            View layoutConfirmacion = findViewById(R.id.layout_confirmacion);
                                            if (layoutConfirmacion != null) {
                                                layoutConfirmacion.setVisibility(View.GONE);
                                            }
                                            BuddySDK.Speech.startSpeaking("Escríbeme tu nombre por favor.", new ITTSCallback.Stub() {
                                                @Override
                                                public void onSuccess(String s) throws RemoteException {
                                                    runOnUiThread(() -> solicitarNombrePorTeclado());
                                                }
                                                @Override public void onError(String error) throws RemoteException {}
                                                @Override public void onPause() throws RemoteException {}
                                                @Override public void onResume() throws RemoteException {}
                                            });
                                        });
                                    });
                                }
                                @Override public void onError(String error) throws RemoteException {
                                    BuddySDK.Speech.startSpeaking("No pude escuchar bien tu nombre. Escribemelo porfi");
                                    runOnUiThread(() -> solicitarNombrePorTeclado());
                                }
                                @Override public void onPause() throws RemoteException {}
                                @Override public void onResume() throws RemoteException {}
                            });
                        }
                    }

                    @Override
                    public void onError(String error) throws RemoteException {
                        BuddySDK.Speech.startSpeaking("No pude escuchar bien tu nombre. Escribemelo porfi");
                        runOnUiThread(() -> solicitarNombrePorTeclado());
                    }
                });
            }

            @Override public void onError(String error) throws RemoteException {}
            @Override public void onPause() throws RemoteException {}
            @Override public void onResume() throws RemoteException {}
        });
    }

    // ---------------------------------------------------------------------------------------------
    // PEDIR NOMBRE POR TECLADO
    // ---------------------------------------------------------------------------------------------
    private void solicitarNombrePorTeclado() {
        runOnUiThread(() -> {
            setContentView(R.layout.teclado);

            EditText inputNombre = findViewById(R.id.edit_nombre);
            Button btnAceptar = findViewById(R.id.btn_aceptar_nombre);

            btnAceptar.setOnClickListener(view -> {
                String nombreManual = inputNombre.getText().toString().trim();

                if (!nombreManual.isEmpty()) {
                    nombreUsuario = nombreManual;
                    View layoutConfirmacion = findViewById(R.id.layout_teclado);
                    if (layoutConfirmacion != null) {
                        layoutConfirmacion.setVisibility(View.GONE);
                    }

                    BuddySDK.Speech.startSpeaking("¡Perfecto " + nombreManual + "! Empecemos a jugar.", new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            BuddySDK.UI.setMood(FacialExpression.HAPPY);
                            runOnUiThread(() -> iniciarAdivinanzaSegunNivel());
                        }

                        @Override public void onError(String error) throws RemoteException {}
                        @Override public void onPause() throws RemoteException {}
                        @Override public void onResume() throws RemoteException {}
                    });
                } else {
                    BuddySDK.Speech.startSpeaking("Por favor, escribe tu nombre.");
                }
            });
        });
    }

    // ---------------------------------------------------------------------------------------------
    // ANÁLISIS BÁSICO DE RESPUESTAS (SÍ/NO)
    // ---------------------------------------------------------------------------------------------
    private String analizarRespuestaPropia(String respuesta) {
        int score = 0;
        String[] positivas = {"sí quiero", "claro", "vale", "por supuesto", "me gustaría"};
        String[] negativas = {"no", "nunca", "no quiero", "tal vez después"};
        for (String palabra : positivas) {
            if (respuesta.contains(palabra)) score++;
        }
        for (String palabra : negativas) {
            if (respuesta.contains(palabra)) score--;
        }
        if (score > 0) return "afirmativa";
        else if (score < 0) return "negativa";
        else return "inconclusa";
    }

    // ---------------------------------------------------------------------------------------------
    // MOVIMIENTO DE CELEBRACIÓN
    // ---------------------------------------------------------------------------------------------
    private void MovimientoCelebracion() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento en progreso, celebración cancelada");
            return;
        }
        movimientoEnProgreso = true;

        BuddySDK.USB.rotateBuddy(130f, 360f, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.d("BuddyMove", "Buddy completó un giro de 360 grados");
                movimientoEnProgreso = false;
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                Log.e("BuddyMove", "Fallo en giro de 360 grados: " + s);
                movimientoEnProgreso = false;
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.celebracion_dos);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
        }, 300);
    }

    // ---------------------------------------------------------------------------------------------
    // MÉTODOS PARA LOS BOTONES DE OPCIONES
    // ---------------------------------------------------------------------------------------------
    public void configurarBotones(String[] opciones, String[] rutasImagenes, String respuestaCorrecta) {
        Button[] botones = {
                findViewById(R.id.btn_opcion1),
                findViewById(R.id.btn_opcion2),
                findViewById(R.id.btn_opcion3),
                findViewById(R.id.btn_opcion4),
                findViewById(R.id.btn_opcion5)
        };
        ImageView[] imagenViews = {
                findViewById(R.id.img_opcion1),
                findViewById(R.id.img_opcion2),
                findViewById(R.id.img_opcion3),
                findViewById(R.id.img_opcion4),
                findViewById(R.id.img_opcion5)
        };

        for (int i = 0; i < botones.length; i++) {
            botones[i].setText(opciones[i]);

            // Cargar imagen desde ruta local
            File imgFile = new File(rutasImagenes[i]);
            if (imgFile.exists()) {
                imagenViews[i].setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            } else {
                imagenViews[i].setImageResource(R.drawable.no_encontrada); // Imagen por defecto
            }

            final int index = i;
            botones[i].setOnClickListener(view ->
                    verificarRespuesta(botones[index].getText().toString(), respuestaCorrecta));
        }

        TextView textoNombre = findViewById(R.id.text_nombre_usuario);
        textoNombre.setText("Jugador: " + nombreUsuario);

        Log.d("LAYOUT", "Estoy en configurarBotones");
        btnPuntuacion = findViewById(R.id.btn_puntuacion);
        if (btnPuntuacion != null) {
            Log.d("LAYOUT", "Encontré btn_puntuacion");
            btnPuntuacion.setText("Puntuación: " + puntuacionTotal);
        } else {
            Log.e("LAYOUT", "NO encontré btn_puntuacion");
        }

        btnRepetir = findViewById(R.id.btn_repetir);
        btnRepetir.setOnClickListener(view -> repetirAdivinanza());
        Button btnTerminar = findViewById(R.id.btn_terminar_partida);
        btnTerminar.setOnClickListener(view -> {
            // Reproducir redoble de tambores antes de mostrar los resultados
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.redoble_tambores);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                // Después de que termine el sonido, mostrar resultados
                mostrarPantallaResultados();
            });
        });
    }


    public void configurarBotones(String[] opciones, String respuestaCorrecta) {
        Button[] botones = {
                findViewById(R.id.btn_opcion1),
                findViewById(R.id.btn_opcion2),
                findViewById(R.id.btn_opcion3),
                findViewById(R.id.btn_opcion4),
                findViewById(R.id.btn_opcion5)
        };

        for (int i = 0; i < botones.length; i++) {
            botones[i].setText(opciones[i]);
            final int index = i;
            botones[i].setOnClickListener(view ->
                    verificarRespuesta(botones[index].getText().toString(), respuestaCorrecta));
        }

        TextView textoNombre = findViewById(R.id.text_nombre_usuario);
        textoNombre.setText("Jugador: " + nombreUsuario);

        btnPuntuacion = findViewById(R.id.btn_puntuacion);
        btnRepetir = findViewById(R.id.btn_repetir);
        btnPuntuacion.setText("Puntuación: " + puntuacionTotal);
        btnRepetir.setOnClickListener(view -> repetirAdivinanza());
        Button btnTerminar = findViewById(R.id.btn_terminar_partida);
        btnTerminar.setOnClickListener(view -> {
            // Reproducir redoble de tambores antes de mostrar los resultados
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.redoble_tambores);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                // Después de que termine el sonido, mostrar resultados
                mostrarPantallaResultados();
            });
        });
    }

    // ---------------------------------------------------------------------------------------------
    // REPETIR ADIVINANZA
    // ---------------------------------------------------------------------------------------------
    public void repetirAdivinanza() {
        BuddySDK.Speech.startSpeaking(preguntaActual, new ITTSCallback.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException { }
            @Override
            public void onError(String error) throws RemoteException {
                BuddySDK.Speech.startSpeaking("Hubo un problema al repetir la adivinanza.");
            }
            @Override
            public void onPause() throws RemoteException { }
            @Override
            public void onResume() throws RemoteException { }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // VERIFICAR RESPUESTA - APLICABLE AL NIVEL BÁSICO / MEDIO CON OPCIONES
    // ---------------------------------------------------------------------------------------------
    public void verificarRespuesta(String respuesta, String correcta) {
        numeroIntentos++;
        if (respuesta.equals(correcta)) {
            aciertosTotales++;
            //statsManager.updateStats(adivinanzaKey, true);
            runOnUiThread(() -> opcionesContainer.setVisibility(View.GONE));
            BuddySDK.Speech.startSpeaking("¡Muy bien!", new ITTSCallback.Stub() {
                @Override
                public void onSuccess(String s) throws RemoteException {
                    runOnUiThread(() -> {
                        BuddySDK.UI.setMood(FacialExpression.HAPPY);

                        if (primerIntento) {
                            puntuacionTotal += 10;
                        } else {
                            puntuacionTotal += 5;
                        }

                        movimientoAleatorioPositivo();
                        eliminarAdivinanzaActual();
                        actualizarPuntuacion();
                        numeroIntentos = 0;
                        primerIntento = true;

                        new Handler(Looper.getMainLooper()).postDelayed(() -> iniciarAdivinanzaSegunNivel(), 3000);
                    });
                }
                @Override
                public void onError(String error) throws RemoteException {
                    BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
                }
                @Override
                public void onPause() throws RemoteException { }
                @Override
                public void onResume() throws RemoteException { }
            });
        } else {
            fallosTotales++;
            if (numeroIntentos == 1) {
                puntuacionTotal -= 5;
                actualizarPuntuacion();
                //statsManager.updateStats(adivinanzaKey, false);
            }
            if (numeroIntentos == 2) {
                puntuacionTotal -= 5;
                actualizarPuntuacion();
                //statsManager.updateStats(adivinanzaKey, false);
                runOnUiThread(() -> opcionesContainer.setVisibility(View.GONE));
                BuddySDK.Speech.startSpeaking("Se acabaron los intentos. La respuesta correcta era " + correcta + ".", new ITTSCallback.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        runOnUiThread(() -> {
                            BuddySDK.UI.setMood(FacialExpression.SAD);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                BuddySDK.UI.setMood(FacialExpression.HAPPY);
                            }, 2000);
                            eliminarAdivinanzaActual();
                            numeroIntentos = 0;
                            primerIntento = true;
                            iniciarAdivinanzaSegunNivel();
                        });
                    }
                    @Override
                    public void onError(String error) throws RemoteException {
                        BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
                    }
                    @Override
                    public void onPause() throws RemoteException { }
                    @Override
                    public void onResume() throws RemoteException { }
                });
            } else {
                runOnUiThread(() -> opcionesContainer.setVisibility(View.GONE));
                BuddySDK.Speech.startSpeaking(obtenerFraseErrorPersonalizada(), new ITTSCallback.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        runOnUiThread(() -> opcionesContainer.setVisibility(View.VISIBLE));
                    }
                    @Override
                    public void onError(String error) throws RemoteException {
                        BuddySDK.Speech.startSpeaking("Hubo un problema al hablar.");
                    }
                    @Override public void onPause() throws RemoteException {}
                    @Override public void onResume() throws RemoteException {}
                });
                BuddySDK.UI.setMood(FacialExpression.THINKING);
                primerIntento = false;
                movimientoAleatorioNegativo();
            }
        }
    }

    private void movimientoAleatorioPositivo() {

        int opcion = new Random().nextInt(3); // 0, 1 o 2

        switch (opcion) {
            case 0:
                Log.d("BuddyMove", "Ejecutando MovimientoCelebracion");
                MovimientoCelebracion();
                break;
            case 1:
                Log.d("BuddyMove", "Ejecutando ejecutarMovimientoSayYes");
                ejecutarMovimientoSayYes();
                break;
            case 2:
                Log.d("BuddyMove", "Ejecutando movimientoSorpresa");
                movimientoSorpresa();
                break;
        }
    }


    private void movimientoCabezaAbajoNegativa() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento en progreso, cancelado");
            return;
        }
        movimientoEnProgreso = true;

        BuddySDK.UI.setMood(FacialExpression.SAD); // expresión neutral o pensativa

        // Reproducir sonido negativo
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.bit_negativo);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release());

        // Paso 1: Mover cabeza hacia abajo
        BuddySDK.USB.buddySayYes(65f, -30f, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.d("BuddyMove", "Cabeza hacia abajo");

                // Esperar 2 segundos (2000 ms) antes de volver al centro
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Paso 2: Volver la cabeza al centro
                    BuddySDK.USB.buddySayYes(65f, 0f, new IUsbCommadRsp.Stub() {
                        @Override
                        public void onSuccess(String s2) throws RemoteException {
                            Log.d("BuddyMove", "Cabeza centrada");
                            runOnUiThread(() -> {
                                BuddySDK.UI.setMood(FacialExpression.NEUTRAL);
                                movimientoEnProgreso = false;
                            });
                        }

                        @Override
                        public void onFailed(String s2) throws RemoteException {
                            movimientoEnProgreso = false;
                        }
                    });
                }, 2000);
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                movimientoEnProgreso = false;
            }
        });
    }

    private void movimientoAleatorioNegativo() {

        int opcion = new Random().nextInt(3); // 0, 1 o 2

        switch (opcion) {
            case 0:
                Log.d("BuddyMove", "Ejecutando movimientoCabezaAbajoNegativa");
                movimientoCabezaAbajoNegativa();
                break;
            case 1:
                Log.d("BuddyMove", "Ejecutando ejecutarMovimientoSayNo");
                ejecutarMovimientoSayNo();
                break;
            case 2:
                Log.d("BuddyMove", "Ejecutando movimientoNegacionDos");
                movimientoNegacionDos();
                break;
        }
    }


    private void movimientoNegacionDos() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento en progreso, negación cancelada");
            return;
        }
        movimientoEnProgreso = true;

        BuddySDK.UI.setMood(FacialExpression.THINKING); // cara de duda o negación

        //Reproducir sonido de respuesta incorrecta
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.respuesta_incorrecta);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release());

        // Paso 1: girar cuerpo hacia la derecha (cabeza también)
        BuddySDK.USB.rotateBuddy(100f, 40f, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s1) throws RemoteException {
                Log.d("BuddyMove", "Cuerpo rotado a la derecha");

                // También rotamos la cabeza ligeramente a la derecha
                BuddySDK.USB.buddySayNo(100f, 30f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s2) throws RemoteException {
                        Log.d("BuddyMove", "Cabeza rotada a la derecha");

                        // Esperamos un poco para que se vea el gesto
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {

                            // Paso 2: volver el cuerpo al centro
                            BuddySDK.USB.rotateBuddy(100f, -3f, new IUsbCommadRsp.Stub() {
                                @Override
                                public void onSuccess(String s3) throws RemoteException {
                                    Log.d("BuddyMove", "Cuerpo centrado");

                                    // Paso 3: volver la cabeza al centro
                                    BuddySDK.USB.buddySayNo(100f, -30f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s4) throws RemoteException {
                                            Log.d("BuddyMove", "Cabeza centrada");
                                            runOnUiThread(() -> {
                                                BuddySDK.UI.setMood(FacialExpression.NEUTRAL);
                                                movimientoEnProgreso = false;
                                            });
                                        }

                                        @Override
                                        public void onFailed(String s4) throws RemoteException {
                                            movimientoEnProgreso = false;
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(String s3) throws RemoteException {
                                    movimientoEnProgreso = false;
                                }
                            });

                        }, 1500); // espera para ver el gesto
                    }

                    @Override
                    public void onFailed(String s2) throws RemoteException {
                        movimientoEnProgreso = false;
                    }
                });
            }

            @Override
            public void onFailed(String s1) throws RemoteException {
                movimientoEnProgreso = false;
            }
        });
    }


    private void movimientoSorpresa() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento bloqueado por otro en progreso");
            return;
        }
        movimientoEnProgreso = true;

        BuddySDK.UI.setMood(FacialExpression.SURPRISED);

        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.sonido_sorpresa);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release());

        // Retroceder
        BuddySDK.USB.moveBuddy(80f, -20f, new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                // Inclinar cabeza atrás
                BuddySDK.USB.buddySayYes(65f, -25f, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s1) throws RemoteException {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Cabeza al centro
                            BuddySDK.USB.buddySayYes(65f, 0f, new IUsbCommadRsp.Stub() {
                                @Override
                                public void onSuccess(String s2) throws RemoteException {
                                    // Avanzar con velocidad moderada
                                    BuddySDK.USB.moveBuddy(0.2f, 20f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s3) throws RemoteException {
                                            // Espera antes de detener
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                // Parada segura
                                                BuddySDK.USB.emergencyStopMotors(new IUsbCommadRsp.Stub() {
                                                    @Override
                                                    public void onSuccess(String s4) throws RemoteException {
                                                        Log.d("BuddyMove", "Motores detenidos");
                                                        runOnUiThread(() -> {
                                                            BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                                            movimientoEnProgreso = false;
                                                        });
                                                    }

                                                    @Override
                                                    public void onFailed(String s4) throws RemoteException {
                                                        Log.e("BuddyMove", "Fallo al detener motores");
                                                        movimientoEnProgreso = false;
                                                    }
                                                });
                                            }, 2200); // Tiempo justo para que recorra 8f a 40f de velocidad
                                        }

                                        @Override
                                        public void onFailed(String s3) throws RemoteException {
                                            movimientoEnProgreso = false;
                                        }
                                    });
                                }

                                @Override
                                public void onFailed(String s2) throws RemoteException {
                                    movimientoEnProgreso = false;
                                }
                            });
                        }, 1500);
                    }

                    @Override
                    public void onFailed(String s1) throws RemoteException {
                        movimientoEnProgreso = false;
                    }
                });
            }

            @Override
            public void onFailed(String s) throws RemoteException {
                movimientoEnProgreso = false;
            }
        });
    }



    // ---------------------------------------------------------------------------------------------
    // MOVIMIENTOS SAY YES / SAY NO
    // ---------------------------------------------------------------------------------------------
    private void ejecutarMovimientoSayYes() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento en progreso, sayYes cancelado");
            return;
        }
        movimientoEnProgreso = true;

        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.correct_answer);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            BuddySDK.USB.buddySayYes(65f, 60f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s1) throws RemoteException {
                    Log.d("BuddyMove", "SayYes - Paso 1: hacia arriba");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        BuddySDK.USB.buddySayYes(65f, -15f, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s2) throws RemoteException {
                                Log.d("BuddyMove", "SayYes - Paso 2: hacia abajo");

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    BuddySDK.USB.buddySayYes(65f, 8f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s3) throws RemoteException {
                                            Log.d("BuddyMove", "SayYes - Paso 3: volver al centro");
                                            movimientoEnProgreso = false;
                                        }
                                        @Override
                                        public void onFailed(String s3) throws RemoteException {
                                            Log.e("BuddyMove", "Fallo al volver al centro (sayYes): " + s3);
                                            movimientoEnProgreso = false;
                                        }
                                    });
                                }, 700);
                            }
                            @Override
                            public void onFailed(String s2) throws RemoteException {
                                Log.e("BuddyMove", "Fallo en paso 2 (hacia abajo sayYes): " + s2);
                                movimientoEnProgreso = false;
                            }
                        });
                    }, 700);
                }
                @Override
                public void onFailed(String s1) throws RemoteException {
                    Log.e("BuddyMove", "Fallo en paso 1 (hacia arriba sayYes): " + s1);
                    movimientoEnProgreso = false;
                }
            });
        }, 400);
    }

    public void ejecutarMovimientoSayNo() {
        if (movimientoEnProgreso) {
            Log.d("BuddyMove", "Movimiento bloqueado por otro en progreso");
            return;
        }
        movimientoEnProgreso = true;

        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.respuesta_incorrecta);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> mp.release());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            BuddySDK.USB.buddySayNo(100f, 25f, new IUsbCommadRsp.Stub() {
                @Override
                public void onSuccess(String s1) throws RemoteException {
                    Log.d("BuddyMove", "Movimiento derecha OK");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        BuddySDK.USB.buddySayNo(100f, -50f, new IUsbCommadRsp.Stub() {
                            @Override
                            public void onSuccess(String s2) throws RemoteException {
                                Log.d("BuddyMove", "Movimiento izquierda OK");

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    BuddySDK.USB.buddySayNo(100f, -0f, new IUsbCommadRsp.Stub() {
                                        @Override
                                        public void onSuccess(String s3) throws RemoteException {
                                            Log.d("BuddyMove", "Cabeza semi centrada tras sayNo");
                                            movimientoEnProgreso = false;
                                        }
                                        @Override
                                        public void onFailed(String s3) throws RemoteException {
                                            Log.e("BuddyMove", "Fallo en ajuste final al centro");
                                            movimientoEnProgreso = false;
                                        }
                                    });
                                }, 800);
                            }
                            @Override
                            public void onFailed(String s2) throws RemoteException {
                                Log.e("BuddyMove", "Fallo en movimiento izquierda");
                                movimientoEnProgreso = false;
                            }
                        });
                    }, 800);
                }
                @Override
                public void onFailed(String s1) throws RemoteException {
                    Log.e("BuddyMove", "Fallo en movimiento derecha");
                    movimientoEnProgreso = false;
                }
            });
        }, 600);
    }

    // ---------------------------------------------------------------------------------------------
    // ACTUALIZAR PUNTUACIÓN Y DETECTAR CAMBIO DE NIVEL
    // ---------------------------------------------------------------------------------------------
    public void actualizarPuntuacion() {
        runOnUiThread(() -> {
            if (btnPuntuacion != null) {
                btnPuntuacion.setText("Puntuación: " + puntuacionTotal);
            } else {
                Log.e("LAYOUT", "btnPuntuacion es null al actualizar.");
            }

            String nuevoNivel = obtenerNivelActual();
            if (!nuevoNivel.equals(nivelAnterior)) {
                String mensaje = "Has cambiado al nivel " + nuevoNivel + ".";

                // Esperamos 3 segundos antes de anunciar el cambio de nivel
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    BuddySDK.Speech.startSpeaking(mensaje, new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            runOnUiThread(() -> iniciarAdivinanzaSegunNivel());
                        }

                        @Override
                        public void onError(String error) throws RemoteException {
                            BuddySDK.Speech.startSpeaking("Hubo un problema al anunciar el nivel.");
                        }

                        @Override
                        public void onPause() throws RemoteException { }

                        @Override
                        public void onResume() throws RemoteException { }
                    });
                }, 3000); // ⏱️ Delay de 3 segundos antes de hablar

                nivelAnterior = nuevoNivel;
            }
        });
    }

    private String obtenerNivelActual() {
        if (puntuacionTotal < 25) {
            return "basico";
        } else if (puntuacionTotal < 45) {
            return "medio";
        } else {
            return "dificil";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // INICIAR ADIVINANZA SEGÚN NIVEL
    // ---------------------------------------------------------------------------------------------
    private void iniciarAdivinanzaSegunNivel() {
        String nivel = obtenerNivelActual();
        switch (nivel) {
            case "basico":
                estadoActual = EstadoConversacion.JUGANDO_BASICO;
                adivinanzasBasicas.iniciarAdivinanza();
                break;
            case "medio":
                estadoActual = EstadoConversacion.JUGANDO_MEDIO;
                adivinanzasMedias.iniciarAdivinanza();
                break;
            case "dificil":
                estadoActual = EstadoConversacion.JUGANDO_DIFICIL;
                adivinanzasDificiles.iniciarAdivinanza();
                break;
            default:
                BuddySDK.Speech.startSpeaking("No se ha determinado el nivel. Comenzaremos con una adivinanza básica.");
                estadoActual = EstadoConversacion.JUGANDO_BASICO;
                adivinanzasBasicas.iniciarAdivinanza();
                break;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Métodos públicos que usan las clases de adivinanzas
    // ---------------------------------------------------------------------------------------------
    public void setPreguntaActual(String pregunta) {
        this.preguntaActual = pregunta;
    }

    public void setPrimerIntento(boolean primerIntento) {
        this.primerIntento = primerIntento;
    }

    public void eliminarAdivinanzaActual() {
        String nivel = obtenerNivelActual();
        switch (nivel) {
            case "basico":
                adivinanzasBasicas.eliminarAdivinanza(adivinanzaKey);
                break;
            case "medio":
                adivinanzasMedias.eliminarAdivinanza(adivinanzaKey);
                break;
            case "dificil":
                adivinanzasDificiles.eliminarAdivinanza(adivinanzaKey);
                break;
            default:
                Log.w("ADIVINANZA", "Nivel desconocido, no se puede eliminar adivinanza");
        }
        Log.d("DEBUG_ADIVINANZA", "Se eliminó del nivel " + nivel + " la adivinanza con key: " + adivinanzaKey);
    }

    // ---------------------------------------------------------------------------------------------
    // NIVEL DIFÍCIL - RESPUESTAS ABIERTAS
    // ---------------------------------------------------------------------------------------------
    public boolean verificarRespuestaDificil(String respuestaUsuario, String[] respuestasValidas) {
        String respuestaNormalizada = respuestaUsuario.toLowerCase().trim();
        for (String valida : respuestasValidas) {
            if (respuestaNormalizada.equals(valida.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }

    public void mostrarInterfazRespuestaDificil(String[] respuestasValidas) {
        runOnUiThread(() -> {
            setContentView(R.layout.nivel_dificil_teclado);

            numeroIntentos = 0;
            primerIntento = true;

            EditText inputRespuesta = findViewById(R.id.edit_respuesta_dificil);
            Button btnEnviarRespuesta = findViewById(R.id.btn_enviar_respuesta_dificil);
            respuestasContainer = findViewById(R.id.respuestas_container);
            btnPuntuacion = findViewById(R.id.btn_puntuacion);
            btnPuntuacion.setText("Puntuación: " + puntuacionTotal);

            btnRepetir = findViewById(R.id.btn_repetir);
            if (btnRepetir != null) {
                btnRepetir.setOnClickListener(view -> repetirAdivinanza());
            }

            Button btnTerminar = findViewById(R.id.btn_terminar_partida);
            btnTerminar.setOnClickListener(view -> {
                // Reproducir redoble de tambores antes de mostrar los resultados
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.redoble_tambores);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    // Después de que termine el sonido, mostrar resultados
                    mostrarPantallaResultados();
                });
            });

            final ISTTCallback[] sttCallback = new ISTTCallback[1];

            // Botón para respuesta por teclado
            btnEnviarRespuesta.setOnClickListener(view -> {
                String respuestaTexto = inputRespuesta.getText().toString().trim();
                detenerSTTActual();
                if (verificarRespuestaDificil(respuestaTexto, respuestasValidas)) {
                    //statsManager.updateStats(adivinanzaKey, true);
                    aciertosTotales++;
                    runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                    BuddySDK.Speech.startSpeaking("¡Muy bien!", new ITTSCallback.Stub() {
                        @Override
                        public void onSuccess(String s) throws RemoteException {
                            runOnUiThread(() -> {
                                BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                if (primerIntento) {
                                    puntuacionTotal += 10;
                                } else {
                                    puntuacionTotal += 5;
                                }
                                actualizarPuntuacion();
                                movimientoAleatorioPositivo();
                                eliminarAdivinanzaActual();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> iniciarAdivinanzaSegunNivel(), 3000);
                            });
                        }

                        @Override public void onError(String error) throws RemoteException {}
                        @Override public void onPause() throws RemoteException {}
                        @Override public void onResume() throws RemoteException {}
                    });
                } else {
                    numeroIntentos++;
                    fallosTotales++;
                    if (numeroIntentos == 1) {
                        puntuacionTotal -= 5;
                        actualizarPuntuacion();
                        //statsManager.updateStats(adivinanzaKey, false);
                    }
                    if (numeroIntentos == 2) {
                        puntuacionTotal -= 5;
                        actualizarPuntuacion();
                        //statsManager.updateStats(adivinanzaKey, false);
                        runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                        BuddySDK.Speech.startSpeaking("Se acabaron los intentos. La respuesta correcta era " + respuestasValidas[0] + ".", new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
                                runOnUiThread(() -> {
                                    BuddySDK.UI.setMood(FacialExpression.SAD);
                                    movimientoAleatorioNegativo();
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                    }, 2000);
                                    eliminarAdivinanzaActual();
                                    numeroIntentos = 0;
                                    primerIntento = true;
                                    iniciarAdivinanzaSegunNivel();
                                });
                            }

                            @Override public void onError(String error) throws RemoteException {}
                            @Override public void onPause() throws RemoteException {}
                            @Override public void onResume() throws RemoteException {}
                        });
                    } else {
                        runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                        BuddySDK.Speech.startSpeaking(obtenerFraseErrorPersonalizada(), new ITTSCallback.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
                                runOnUiThread(() -> {
                                    respuestasContainer.setVisibility(View.VISIBLE);

                                    // Reactivar STT después de fallo escrito
                                    detenerSTTActual();
                                    sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
                                    sttTask.start(false, sttCallback[0]);
                                });
                            }

                            @Override public void onError(String error) throws RemoteException {}
                            @Override public void onPause() throws RemoteException {}
                            @Override public void onResume() throws RemoteException {}
                        });
                        BuddySDK.UI.setMood(FacialExpression.THINKING);
                        primerIntento = false;
                        movimientoAleatorioNegativo();
                    }
                }
            });

            detenerSTTActual();

            sttCallback[0] = new ISTTCallback.Stub() {
                @Override
                public void onSuccess(STTResultsData resultsData) throws RemoteException {
                    if (resultsData.getResults() != null && !resultsData.getResults().isEmpty()) {
                        String respuestaVoz = resultsData.getResults().get(0).getUtterance();
                        Log.d("NIVEL_DIFICIL", "Respuesta reconocida por voz: " + respuestaVoz);

                        if (verificarRespuestaDificil(respuestaVoz, respuestasValidas)) {
                            //statsManager.updateStats(adivinanzaKey, true);
                            aciertosTotales++;
                            runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                            BuddySDK.Speech.startSpeaking("¡Muy bien!", new ITTSCallback.Stub() {
                                @Override
                                public void onSuccess(String s) throws RemoteException {
                                    runOnUiThread(() -> {
                                        BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                        if (primerIntento) {
                                            puntuacionTotal += 10;
                                        } else {
                                            puntuacionTotal += 5;
                                        }
                                        actualizarPuntuacion();
                                        movimientoAleatorioPositivo();
                                        eliminarAdivinanzaActual();
                                        numeroIntentos = 0;
                                        primerIntento = true;
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> iniciarAdivinanzaSegunNivel(), 3000);
                                    });
                                }

                                @Override public void onError(String error) throws RemoteException {}
                                @Override public void onPause() throws RemoteException {}
                                @Override public void onResume() throws RemoteException {}
                            });
                        } else {
                            numeroIntentos++;
                            fallosTotales++;
                            if (numeroIntentos == 1) {
                                puntuacionTotal -= 5;
                                actualizarPuntuacion();
                                //statsManager.updateStats(adivinanzaKey, false);
                            }
                            if (numeroIntentos == 2) {
                                puntuacionTotal -= 5;
                                actualizarPuntuacion();
                                //statsManager.updateStats(adivinanzaKey, false);
                                runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                                BuddySDK.Speech.startSpeaking("Se acabaron los intentos. La respuesta correcta era " + respuestasValidas[0] + ".", new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        runOnUiThread(() -> {
                                            BuddySDK.UI.setMood(FacialExpression.SAD);
                                            movimientoAleatorioNegativo();
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                BuddySDK.UI.setMood(FacialExpression.HAPPY);
                                            }, 2000);
                                            eliminarAdivinanzaActual();
                                            numeroIntentos = 0;
                                            primerIntento = true;
                                            iniciarAdivinanzaSegunNivel();
                                        });
                                    }

                                    @Override public void onError(String error) throws RemoteException {}
                                    @Override public void onPause() throws RemoteException {}
                                    @Override public void onResume() throws RemoteException {}
                                });
                            } else {
                                runOnUiThread(() -> respuestasContainer.setVisibility(View.GONE));
                                BuddySDK.Speech.startSpeaking(obtenerFraseErrorPersonalizada(), new ITTSCallback.Stub() {
                                    @Override
                                    public void onSuccess(String s) throws RemoteException {
                                        runOnUiThread(() -> {
                                            respuestasContainer.setVisibility(View.VISIBLE);
                                            detenerSTTActual();
                                            sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
                                            sttTask.start(false, sttCallback[0]);
                                        });
                                    }

                                    @Override public void onError(String error) throws RemoteException {}
                                    @Override public void onPause() throws RemoteException {}
                                    @Override public void onResume() throws RemoteException {}
                                });
                                BuddySDK.UI.setMood(FacialExpression.THINKING);
                                primerIntento = false;
                                movimientoAleatorioNegativo();
                            }
                        }
                    }
                }

                @Override
                public void onError(String error) throws RemoteException {
                    BuddySDK.Speech.startSpeaking("No pude entenderte, intenta de nuevo.");
                }
            };

            sttTask = BuddySDK.Speech.createGoogleSTTTask(Locale.forLanguageTag("es-ES"));
            sttTask.start(false, sttCallback[0]);
        });
    }


}
