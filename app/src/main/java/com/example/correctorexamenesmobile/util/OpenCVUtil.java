package com.example.correctorexamenesmobile.util;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class OpenCVUtil {
    private static final String TAG = "OpenCVUtil";

    public static Mat setAnadirBlurALaImagen(Mat imagen) {
        Mat imagenConBlur = new Mat();

        try {
            // Aplicamos el filtro Gaussiano a la imagen para su suavizado
            Imgproc.GaussianBlur(imagen, imagenConBlur, new Size(5, 5), 0);
            return imagenConBlur;

        } catch (Exception e) {
            Log.e(TAG, "Error al aplicar blur: " + e.getMessage());
            return imagen;
        }
    }

    public static Mat setConvertirEscalaGrises(Mat imagenOriginal) {
        Mat imagenEscalaGrises = new Mat();

        try {
            Imgproc.cvtColor(imagenOriginal, imagenEscalaGrises, Imgproc.COLOR_BGR2GRAY);
            return imagenEscalaGrises;

        } catch (Exception e) {
            Log.e(TAG, "Error al convertir a escala de grises: " + e.getMessage());
            return imagenOriginal;
        }
    }

    public static Mat setAplicarCanny(Mat imagenEscalaGrises) {
        Mat imagenConBordes = new Mat();

        try {
            Imgproc.Canny(imagenEscalaGrises, imagenConBordes, 100, 200);
            return imagenConBordes;

        } catch (Exception e) {
            Log.e(TAG, "Error al aplicar Canny: " + e.getMessage());
            return imagenEscalaGrises;
        }
    }

    public static Mat setBinarizarImagen(Mat imagenGris) {
        try {

            Mat imagenBinarizada = new Mat();
            Imgproc.adaptiveThreshold(
                    imagenGris,
                    imagenBinarizada,
                    255,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C,
                    Imgproc.THRESH_BINARY,
                    11,
                    2
            );

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
            Mat imagenLimpia = new Mat();
            Mat imagenFinal = new Mat();

            Imgproc.morphologyEx(imagenBinarizada, imagenLimpia, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 1);
            Imgproc.morphologyEx(imagenBinarizada, imagenFinal, Imgproc.MORPH_CLOSE, kernel);

            return imagenFinal;

        } catch (Exception e) {
            Log.e(TAG, "Error al binarizar imagen: " + e.getMessage());
            return imagenGris;
        }
    }

    public static int procesarCirculos(Mat imagenColor, Rect rect) {
        try {

            Mat circuloRecortado = imagenColor.submat(rect);
            Mat circuloEscalaDeGrises = new Mat();
            Imgproc.cvtColor(circuloRecortado, circuloEscalaDeGrises, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(circuloEscalaDeGrises, circuloEscalaDeGrises, new Size(3, 3), 0);

            Mat circuloBinarizado = new Mat();
            Imgproc.threshold(circuloEscalaDeGrises, circuloBinarizado, 128, 255, Imgproc.THRESH_BINARY_INV);

            return contarPixelesNegros(circuloBinarizado);

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar círculos: " + e.getMessage());
            return 0;
        }
    }

    private static int contarPixelesNegros(Mat imagenBinarizada) {

        int count = 0;

        try {
            for (int i = 0; i < imagenBinarizada.rows(); i++) {
                for (int j = 0; j < imagenBinarizada.cols(); j++) {
                    double[] pixel = imagenBinarizada.get(i, j);
                    if (pixel[0] == 255) {
                        count++;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al contar píxeles negros: " + e.getMessage());
        }
        return count;
    }

    public static String calcularNota(ArrayList<Character> respuestasCorrectas, ArrayList<Character> respuestasAlumno) {

        try {
            int A = 0;
            int E = 0;
            int k = 4;
            int totalPreguntas = respuestasCorrectas.size();

            for (int i = 0; i < totalPreguntas; i++) {
                if (respuestasAlumno.get(i) == 'X') {
                    continue;
                }
                if (Objects.equals(respuestasAlumno.get(i), respuestasCorrectas.get(i))) {
                    A++;
                } else {
                    E++;
                }
            }

            double nota = A - (double) E / (k - 1);
            double notaBase10 = (nota / totalPreguntas) * 10.0;

            return String.format(Locale.getDefault(), "%.2f", notaBase10);

        } catch (Exception e) {
            Log.e(TAG, "Error al calcular nota: " + e.getMessage());
            return "0.00";
        }
    }
}
