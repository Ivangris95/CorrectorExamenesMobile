package com.example.correctorexamenesmobile.ui.test;

import static com.example.correctorexamenesmobile.domain.processor.ExamImageProcessor.cargarImagen;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import android.Manifest;

import org.opencv.core.Mat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.correctorexamenesmobile.R;

public class TestOpenCVActivity extends AppCompatActivity {

    private static final String TAG = "OpenCVLog";

    // Bloque static para cargar la biblioteca nativa
    static {
        if (!org.opencv.android.OpenCVLoader.initLocal()) {
            // Error al cargar OpenCV
            Log.e(TAG, "No se pudo cargar OpenCV");
        } else {
            // OpenCV cargado correctamente
            Log.i(TAG, "OpenCV cargado exitosamente");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_opencv);


        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        String ruta = "\\sdcard\\Pictures\\003-completo.jpg";
        Mat imagen = cargarImagen(ruta);
        Log.i(TAG, "Imagen: " + imagen);*/
    }
}
