package com.example.correctorexamenesmobile.domain.processor;

import android.util.Log;

import com.example.correctorexamenesmobile.util.OpenCVUtil;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;

public class ExamImageProcessor {
    private static final String TAG = "CorrectorExamenes";

    // Listas estáticas para almacenar resultados
    private static final ArrayList<ArrayList<Integer>> listaDePixelesPorGruposNumeroExamen = new ArrayList<>();
    private static final ArrayList<Integer> listaPixelesLetrasIdentificador = new ArrayList<>();
    private static final ArrayList<ArrayList<Integer>> listaDePixelesPorGruposIdentificador = new ArrayList<>();
    private static final ArrayList<ArrayList<Integer>> listaDePixelesPorGrupos = new ArrayList<>();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static Mat cargarImagen(String ruta) {
        Mat imagen = Imgcodecs.imread(ruta);
        if (imagen.empty()) {
            Log.e(TAG, "Error al cargar la imagen: " + ruta);
            return null;
        }
        return imagen;
    }

    public static Mat procesarImagen(Mat imagen) {
        try {
            Mat imagenEscalaGrises = OpenCVUtil.setConvertirEscalaGrises(imagen);
            Mat imagenConBlur = OpenCVUtil.setAñadirBlurALaImagen(imagenEscalaGrises);
            Mat imagenBinarizada = OpenCVUtil.setBinarizarImagen(imagenConBlur);
            return OpenCVUtil.setAplicarCanny(imagenBinarizada);
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar imagen: " + e.getMessage());
            return null;
        }
    }
}
