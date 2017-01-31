package com.mengroba.camerawebview01;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.mengroba.camerawebview01.MainActivity;

import java.net.URISyntaxException;

import static android.content.Intent.getIntent;

/**
 * Created by mengroba on 25/01/2017.
 */

public class WebAppInterface {

    private Context context;
    private static final String URLGOOGLE =
            "https://www.google.es/search?sourceid=chrome-psyapi2&rlz=1C1CAFA_enES728ES728&ion=1&espv=2&ie=UTF-8&q=";
    private static final String URLMAP1 = "https://www.google.es/maps/place//@";
    private static final String URLMAP2 = ",15z/data=!4m5!3m4!1s0x0:0x0!8m2!3d";
    private static final int STATE_SEARCH = 1;
    private static final int STATE_MAP = 2;
    private static final int STATE_SCAN = 3;
    private Intent intent;
    private WebView webView2;

    /**
     * Constructor de la clase WebAppInterface
     *
     * @param context
     */
    public WebAppInterface(Context context) {
        this.context = context;
    }

    /**
     * Muestra un cuadro de dialogo del mensaje pasado por parametro en el HTML
     * @param msg
     */
    @JavascriptInterface
    public void showDialog(String msg) {
        //Usamos una clase Builder para la construccion del dialogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(msg).setNeutralButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        //Creamos el dialogo
        builder.create().show();
    }

    /**
     * Creamos un Toast con el mensaje pasado por parametro en el HTML     *
     * @param msg
     */
    public void makeToastAndroid(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Accedemos al modulo de captura de codigo de barras
     */
    public void scanBarcode() {
        //Pasamos a MainActivity el estado correspondiente al escaner de barras
        intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("STATE", STATE_SCAN);
        context.startActivity(intent);
    }

    /**
     * Metodo que lanza el buscador cuando se presiona el boton correspondiente en el HTML     *
     * @param texto
     */
    public void showWebPage(String texto) {
        //Pasamos a MainActivity el texto a buscar
        intent = new Intent(context, MainActivity.class);
        intent.putExtra("STATE", STATE_SEARCH);
        intent.putExtra("SEARCH", URLGOOGLE + texto);
        context.startActivity(intent);
    }

    /**
     * Metodo que carga un mapa con las coordenadas indicadas por el usuario     *
     * @param lat
     * @param lon
     */
    public void showMap(String lat, String lon) {
        //Pasamos a MainActivity las coordenadas a buscar
        intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("STATE", STATE_MAP);
        intent.putExtra("COORD", URLMAP1 + lat + 0 + lon + 0 + URLMAP2 + lat + "," + lon);
        context.startActivity(intent);
    }
}
