package com.mengroba.camerawebview01;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
//Imports para saber el tiempo de carga de la pagina
//import android.graphics.Bitmap;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.sudar.zxingorient.ZxingOrient;
import me.sudar.zxingorient.ZxingOrientResult;


public class MainActivity extends AppCompatActivity {

    //TAG para el Log Info
    private static final String TAG = "MainActivity";
    private static final int STATE_HOMEPAGE = 0;
    private static final int STATE_SEARCH = 1;
    private static final int STATE_MAP = 2;
    private static final int STATE_SCAN = 3;

    private WebView webView;
    private ProgressBar progressBar;
    private int state;
    public long loadTime; //indica el tiempo de carga de la pagina

    //creamos la constante de direccion web
    //Prueba: private static final String URL_WEB = "http://www.google.es";
    private String url;

    private static final int FILECHOOSER_RESULTCODE = 100;
    private static final int BARCODE_RESULTCODE = 49374;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri> mUploadMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fijamos el layout a utilizar
        setContentView(R.layout.activity_main);

        //Enlazamos los elementos graficos
        webView = (WebView) findViewById(R.id.webView1);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        // Definimos la url de inicio, dentro de la carpeta assets
        String webViewUrl = "file:///android_asset/prueba.html";

        //Enlazamos WebAppInterface entre el codigo JavaScript y el codigo Android
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        //Accedemos a la configuracion del WebView y habilitamos el javascript
        webView.getSettings().setJavaScriptEnabled(true);

        // Ajustamos el HTML al WebView
        webView.getSettings().setLoadWithOverviewMode(true);
        //añadimos scroll
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        //habilitamos las opciones de zoom
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        //Si queremos habilitar plugins al WebView (no se recomienda por seguridad)
        //webView.getSettings().setPluginState(WebSettings.PluginState.OFF);
        //habilitamos el tratamiento de ficheros
        webView.getSettings().setAllowFileAccess(true);

        //Cargamos el WebView segun las elecciones en el HTML
        //Por defecto se cargaria siempre la pagina principal
        state = getIntent().getIntExtra("STATE", STATE_HOMEPAGE);
        switch (state) {
            case STATE_HOMEPAGE:
                webView.loadUrl(webViewUrl);
                break;
            //Si se ha pulsado el boton de busqueda
            case STATE_SEARCH:
                url = getIntent().getStringExtra("SEARCH");
                webView.loadUrl(url);
                break;
            //Si se ha pulsado el boton de mapa
            case STATE_MAP:
                url = getIntent().getStringExtra("COORD");
                webView.loadUrl(url);
                break;
            //Si se ha pulsado el boton de mapa
            case STATE_SCAN:
                new ZxingOrient(MainActivity.this).initiateScan();
                break;
            default:
                webView.loadUrl(webViewUrl);
                break;
        }

        // definimos el navegador
        startWebView();

    }

    /**
     * Creamos un cliente de WebView que es llamado al abrir la url     *
     */
    private void startWebView() {

        // Accedemos al WebClient como clase interna
        webView.setWebViewClient(new WebViewClient() {
            ProgressDialog progressDialog;

            /**
             * Sobreescribimos el metodo shouldOverrideUrlLoading para editar el comportamiento
             * del WebView al cargar una url que contenga un texto especifico.
             * --Este metodo quedara obsoleto a partir de la API24, en nuestro caso no nos afecta.--
             * @param view
             * @param url
             * @return
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                /*Si los enlaces contienen el string indicado, entonces se visualizaran
                  con una aplicacion exterior*/
                if (url.contains("androidtest")) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    //ademas limpiamos la pila de activitys
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    finish();
                    return true;
                    //De otra forma, se abren en el propio WebView
                } else {
                    view.loadUrl(url);
                    return true;
                }

            }
        });

        /* Definimos una clase interna WebChrome Client y un metodo openFileChooser para
         seleccionar un archivo desde la aplicacion de camara o del almacenamiento.*/
        webView.setWebChromeClient(new WebChromeClient() {

            /**
             * Mostrar la carga de la pagina web
             * @param view
             * @param progress
             */
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
                MainActivity.this.setProgress(progress * 1000);

                progressBar.incrementProgressBy(progress);

                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            /*Metodos si queremos saber el tiempo de carga de la web
            /*@Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                // Guardamos el tiempo de inicio
                loadTime = currentTimeMillis();

                // Mostramos el mensaje
                Toast.makeText(getApplicationContext(),
                        "Cargando la pagina...", LENGTH_SHORT).show();
            }*/
            /*@Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Calculamos el tiempo de carga
                this.loadTime = currentTimeMillis() - loadTime;

                // Convertimos los milisegundos a min:seg:ms
                String time = new SimpleDateFormat("mm:ss:SSS", Locale.getDefault())
                        .format(new Date(loadTime));

                // Mostramos el tiempo de carga en el log
                Log.i(TAG,
                        "El tiempo de carga ha sido de " + time);
            }*/

            /**
             * Diferentes metodos de selector de imagenes, segun al API del dispositivo
             */
            // openFileChooser Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

                // Definimos el mensaje del selector de imagenes
                mUploadMessage = uploadMsg;

                try {
                    // Creamos el directorio AndroidTestFolder en la carpeta Pictures publica.
                    File imageStorageDir = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES)
                            , "AndroidTestFolder");
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }

                    // Configuramos el nombre del archivo de la imagen
                    loadTime = System.currentTimeMillis();
                    String time = new SimpleDateFormat("HHmmssSSS", Locale.getDefault())
                            .format(new Date(loadTime));
                    File file = new File(
                            imageStorageDir + File.separator + "IMG_"
                                    + String.valueOf(time)
                                    + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file);

                    // Lanzamos el intent de la aplicacion de la camara
                    final Intent captureIntent = new Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    // Ponemos un titulo al elegir la imagen
                    Intent chooserIntent = Intent.createChooser(i, "Elige imagen");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                            , new Parcelable[]{captureIntent});

                    // Llamamos a la actividad al elegir el archivo
                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "Error en carga de imagen:" + e,
                            Toast.LENGTH_LONG).show();
                }
            } //Fin de openFileChooser Android 3.0+

            // openFileChooser Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            //openFileChooser para versiones antiguas de Android
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

            /**
             * Enviamos un mensaje por Javascript en caso de error en el WebChromeClient
             * @param cm
             * @return
             */
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                onConsoleMessage(cm.message(), cm.lineNumber(), cm.sourceId());
                return true;
            }

            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, "Mostrando mensaje de consola: " + message);

            }

            /**
             * Permisos para el callback de la Geolocalizacion
             * @param origin
             * @param callback
             */
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });   // Fin de setWebChromeClient

    } //Fin de startWebView

    /**
     * Metodo ejecutado con el resultado del {@link Activity#startActivityForResult}, segun su utilizacion
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //requestCode correspondiente a la camara
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (this.mUploadMessage == null) {
                return;
            }

            Uri result = null;

            try {
                if (resultCode != RESULT_OK) {
                    result = null;
                } else {
                    result = intent == null ? mCapturedImageURI : intent.getData();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "activity :" + e,
                        Toast.LENGTH_LONG).show();
            }
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;

            //requestCode correspondiente al scanner de codigos de barras
        } else if (requestCode == BARCODE_RESULTCODE) {
            //Cargamos la libreria Zxing a traves de scanResult y parseamos el resultado
            ZxingOrientResult scanResult = ZxingOrient.parseActivityResult(requestCode, resultCode, intent);
            if (scanResult != null && scanResult.getContents() != null) {
                /*Pasamos la informacion al usuario, para ello usamos un dialogo emergente*/
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("El formato es: " + scanResult.getFormatName() + "\n" +
                        "él codigo es: " + scanResult.getContents())
                        .setNeutralButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                //Creamos el dialogo
                builder.create().show();

            }
        }
    }

    /**
     * Sobreescribimos el metodo de boton vuelta atras para que vaya a la anterior pagina visitada
     */
    @Override
    public void onBackPressed() {

        //Comprobamos si hay historial de navegacion
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Si no lo hay, damos el control al Back de la Activity
            super.onBackPressed();
        }
    }

}
