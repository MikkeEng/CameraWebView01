package com.mengroba.camerawebview01;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
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

import me.sudar.zxingorient.Barcode;
import me.sudar.zxingorient.ZxingOrient;
import me.sudar.zxingorient.ZxingOrientResult;


/**
 * Clase principal de proyecto
 */
public class MainActivity extends AppCompatActivity {

    //TAG para el Log Info
    private static final String TAG = "MainActivity";
    private static final int STATE_HOMEPAGE = 0;
    private static final int STATE_SEARCH = 1;
    private static final int STATE_MAP = 2;
    private static final int STATE_SCAN = 3;
    private static final int STATE_CAMERA = 4;
    private static final int REQUEST_IMAGE_CAPTURE = 5;
    private static final String WEB_HOME= "file:///android_asset/prueba.html";

    public WebView webView;
    private ProgressBar progressBar;
    private int state;
    public long loadTime; //indica el tiempo de carga de la pagina

    //creamos la constante de direccion web
    //Prueba: private static final String URL_WEB = "http://www.google.es";
    private String url;
    private ZxingOrient scanner;
    private String scanContentResut;
    private String scanFormatResut;

    private static final int FILECHOOSER_RESULTCODE = 100;
    private static final int BARCODE_RESULTCODE = 49374;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri> mUploadMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fijamos el layout a utilizar
        setContentView(R.layout.activity_main);
        //creamos el visor HTML
        createWebView(this);
        //Cargamos el WebView segun las elecciones en el HTML
        //Por defecto se cargaria siempre la pagina principal
        state = getIntent().getIntExtra("STATE", STATE_HOMEPAGE);
        switch (state) {
            case STATE_HOMEPAGE:
                webView.loadUrl(WEB_HOME);
                break;
            //Si se ha pulsado el boton de busqueda
            case STATE_CAMERA:
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
                finish();
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
            /*Si se ha pulsado el boton de escanear codigo de barras, se hace uso de la
            libreria zxing para lanzar el escaner*/
            case STATE_SCAN:
                //se edita el layout del scanner
                scanner = new ZxingOrient(MainActivity.this);
                scanner.setToolbarColor("#1c1c1c");
                scanner.setIcon(R.drawable.ic_barcode_scan);
                scanner.setInfo("Pulsa ATRÁS para cancelar");
                scanner.setInfoBoxColor("#1c1c1c");
                scanner.setBeep(true).initiateScan(Barcode.ONE_D_CODE_TYPES, -1);
                break;
            default:
                webView.loadUrl(WEB_HOME);
                break;
        } //Fin del switch

        // definimos el visor HTML
        startWebView();

    }

    /**
     * Creamos el visor WebView para cargar el HTML
     */
    private void createWebView(Context context){

        //Enlazamos los elementos graficos
        webView = (WebView) findViewById(R.id.webView1);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        //Enlazamos WebAppInterface entre el codigo JavaScript y el codigo Android
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        //Accedemos a la configuracion del WebView y habilitamos el javascript
        webView.getSettings().setJavaScriptEnabled(true);
        // Ajustamos el HTML al WebView
        webView.getSettings().setLoadWithOverviewMode(true);
        //añadimos scroll
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        //habilitamos el uso de mediaplayer sin gestos
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        //habilitamos las opciones de zoom
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        //Si queremos habilitar plugins al WebView (no se recomienda por seguridad)
        //webView.getSettings().setPluginState(WebSettings.PluginState.OFF);
        //habilitamos el tratamiento de ficheros
        webView.getSettings().setAllowFileAccess(true);
    }

    /**
     * Iniciamos un cliente de WebView que es llamado al abrir el HTML
     */
    private void startWebView() {

        // Accedemos al WebClient como clase interna
        webView.setWebViewClient(new WebViewClient() {

            ProgressDialog progressDialog;

            /**
             * Metodo sobreescrito para probar el envio de parametros Android->Javascript
             * @param view
             * @param url
             */
            @Override
            public void onPageFinished(WebView view, String url) {
                String prueba = "Escaner Codigo de Barras";
                view.loadUrl("javascript:scanBarcodeResult(\""+prueba+"\")");
            }

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
        });  //Fin del WebClient

        /**
         * Definimos una clase interna WebChrome Client y un metodo openFileChooser para
         * seleccionar un archivo desde la aplicacion de camara o del almacenamiento.
         */
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                Log.i(TAG, "onPermissionRequest");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

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

            /*Metodos si queremos saber el tiempo de carga de la web/*
            @Override
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
    protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {

        //requestCode correspondiente a la camara
        if (requestCode == FILECHOOSER_RESULTCODE || requestCode == REQUEST_IMAGE_CAPTURE) {
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
            scanFormatResut = scanResult.getFormatName();
            scanContentResut = scanResult.getContents();
            Log.i(TAG, "onActivityResult" + scanFormatResut);
            Log.i(TAG, "onActivityResult" + scanContentResut);
            if (scanResult.getContents() != null) {
                //creamos el visor HTML
                //createWebView(this);
                //webView.loadUrl("https://www.google.es");
                webView.loadUrl("javascript:alert(\"Dialog HTML: \n El formato es "+scanFormatResut+"\n" +
                        " y el codigo es: " + scanContentResut+"\")");
                webView.loadUrl("javascript:scanResult(\""+scanContentResut+"\")");
                /*System.out.println(scanContentResut);
                System.out.println(scanFormatResut);*/
                //Pasamos la informacion al usuario, para ello usamos un dialogo emergente
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setMessage("El formato es: " + scanFormatResut + "\n" +
                        "y el codigo es: " + scanContentResut)
                        .setPositiveButton("Guardar codigo", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "Guardando codigo", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainActivity.this, "Cancelando operación...", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                finish();
                            }
                        });
                //Creamos el dialogo
                builder.create().show();
                //TODO: Conseguir pasar el valor del codigo al campo de texto id_scan en el WebView

            } else {
                Toast.makeText(this, "No se ha obtenido ningun dato", Toast.LENGTH_SHORT).show();
                finish();
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
