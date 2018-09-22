package com.example.jonathan.xkcd_comic;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jonathan.xkcd_comic.com.example.jonathan.xkcd_comic.core.AbsRuntimePermission;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.nostra13.universalimageloader.utils.DiskCacheUtils.findInCache;
import static com.nostra13.universalimageloader.utils.DiskCacheUtils.removeFromCache;

public class MainActivity extends AbsRuntimePermission {
    private static final int REQUEST_PERMISSION = 1;
    private static final String TAG = "xkdc.main";

    ImageButton btnPrimero;
    ImageButton btnAnterior;
    ImageButton btnSiguiente;
    ImageButton btnUltimo;
    ImageView imagen;
    TextView txtTitulo;
    TextView txtDesc;

    int comicActual=-1;
    int comicHoy;


    private ConsumoServicio mConsumoServicio = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)) {
            requestAppPermissions(new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    R.string.msg, REQUEST_PERMISSION);
        } else {
            init();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        init();
    }

    public void init() {
        setContentView(R.layout.activity_main);
        btnPrimero = findViewById(R.id.btnPrimero);
        btnPrimero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConsumoServicio = new ConsumoServicio(1);
                mConsumoServicio.execute((Void) null);
            }
        });
        btnAnterior = findViewById(R.id.btnAnterior);
        btnAnterior.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConsumoServicio = new ConsumoServicio(comicActual-1);
                mConsumoServicio.execute((Void) null);
            }
        });
        btnSiguiente = findViewById(R.id.btnSiguiente);
        btnSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConsumoServicio = new ConsumoServicio(comicActual+1);
                mConsumoServicio.execute((Void) null);

            }
        });
        btnUltimo = findViewById(R.id.btnUltimo);
        btnUltimo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mConsumoServicio = new ConsumoServicio(comicHoy);
                mConsumoServicio.execute((Void) null);

            }
        });
        imagen = findViewById(R.id.imgFoto);
        txtTitulo = findViewById(R.id.txtTitulo);
        txtDesc = findViewById(R.id.txtDesc);

        if(comicActual==-1){
            mConsumoServicio = new ConsumoServicio(-1);
            mConsumoServicio.execute((Void) null);
        }

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ConsumoServicio extends AsyncTask<Void, Void, Boolean> {
        int numComic;
        JSONObject result;
        boolean hoy = false;
        ConsumoServicio(int nc) {
             this.numComic += nc;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bandera = false;
            HttpURLConnection connection = null;
            BufferedReader reader;
            try {
                URL url;
                if (numComic==-1){
                 url = new URL(getResources().getString(R.string.url ) + "/info.0.json");
                 hoy = true;
                }else{
                    url = new URL(getResources().getString(R.string.url )+"/"+ numComic + "/info.0.json");
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Content-type", "application/json");
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String data;
                    StringBuffer stringBuffer = new StringBuffer();
                    while ((data = reader.readLine()) != null) {
                        stringBuffer.append(data);
                    }
                    result = new JSONObject(stringBuffer.toString());
                    numComic = result.getInt("num");
                    comicActual = numComic;
                    if(hoy){comicHoy = comicActual;}
                    bandera = true;
                }
            } catch (Exception e) {
                e.getStackTrace();
                Log.e(TAG, e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return bandera;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mConsumoServicio = null;
            //showProgress(false);
            if (success) {
                switchBotones();
                try {
                    txtTitulo.setText(result.getString("title"));
                    loadImgLogo(result.getString("img"));
                    txtDesc.setText(String.valueOf(result.getInt("num")));
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }else{
                if(comicActual==403){
                    mConsumoServicio = new ConsumoServicio(405);
                    mConsumoServicio.execute((Void) null);
                }else if(comicActual==405){
                    mConsumoServicio = new ConsumoServicio(403);
                    mConsumoServicio.execute((Void) null);
                }
                Log.e(TAG, String.valueOf(comicActual));

            }
        }
    }

    public void switchBotones(){
        if(comicActual==1){
            habilitaBotones(1);
        }else if(comicActual==comicHoy){
            habilitaBotones(2);
        }else{
            habilitaBotones(3);
        }
    }

    public void habilitaBotones(int opcion){
        switch (opcion){
            case 1:
                btnPrimero.setEnabled(false);
                btnAnterior.setEnabled(false);
                btnPrimero.setBackgroundColor(Color.RED);
                btnAnterior.setBackgroundColor(Color.RED);
                btnSiguiente.setEnabled(true);
                btnUltimo.setEnabled(true);
                btnSiguiente.setBackgroundColor(android.R.drawable.btn_default);
                btnUltimo.setBackgroundColor(android.R.drawable.btn_default);

                break;
            case 2:
                btnUltimo.setEnabled(false);
                btnSiguiente.setEnabled(false);
                btnUltimo.setBackgroundColor(Color.RED);
                btnSiguiente.setBackgroundColor(Color.RED);
                btnPrimero.setEnabled(true);
                btnAnterior.setEnabled(true);
                btnPrimero.setBackgroundColor(android.R.drawable.btn_default);
                btnAnterior.setBackgroundColor(android.R.drawable.btn_default);
                break;
            case 3:
                btnPrimero.setEnabled(true);
                btnAnterior.setEnabled(true);
                btnSiguiente.setEnabled(true);
                btnUltimo.setEnabled(true);
                btnPrimero.setBackgroundColor(android.R.drawable.btn_default);
                btnAnterior.setBackgroundColor(android.R.drawable.btn_default);
                btnSiguiente.setBackgroundColor(android.R.drawable.btn_default);
                btnUltimo.setBackgroundColor(android.R.drawable.btn_default);
                break;
        }
    }

    private void loadImgLogo(String img) {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(MainActivity.this));
        imageLoader.displayImage(img, imagen, new SimpleImageLoadingListener() {
            boolean cacheFound;

            @Override
            public void onLoadingStarted(String url, View view) {
                List<String> memCache = MemoryCacheUtils.findCacheKeysForImageUri(url, ImageLoader.getInstance().getMemoryCache());
                cacheFound = !memCache.isEmpty();
                if (!cacheFound) {
                    File discCache = findInCache(url, ImageLoader.getInstance().getDiskCache());
                    if (discCache != null) {
                        cacheFound = discCache.exists();
                    }
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (cacheFound) {
                    MemoryCacheUtils.removeFromCache(imageUri, ImageLoader.getInstance().getMemoryCache());
                    removeFromCache(imageUri, ImageLoader.getInstance().getDiskCache());
                    ImageLoader.getInstance().displayImage(imageUri, (ImageView) view);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("comicActual", comicActual);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        comicActual = savedInstanceState.getInt("comicActual");
    }
}
