package com.example.correctorexamenesmobile.domain.processor;

import android.util.Log;

import com.example.correctorexamenesmobile.data.api.model.Circulos;
import com.example.correctorexamenesmobile.util.OpenCVUtil;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

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
            Mat imagenConBlur = OpenCVUtil.setAnadirBlurALaImagen(imagenEscalaGrises);
            Mat imagenBinarizada = OpenCVUtil.setBinarizarImagen(imagenConBlur);

            return OpenCVUtil.setAplicarCanny(imagenBinarizada);
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar imagen: " + e.getMessage());
            return null;
        }
    }

    public static List<Rect> detectarYOrdenarCincoContornosPrincipales(Mat imagenConBordes) {
        try {
            List<MatOfPoint> listaContornos = new ArrayList<>();
            List<Rect> listaRectangulosContornos = new ArrayList<>();
            List<Rect> listaRectangulos = new ArrayList<>();

            // Encontrar contornos
            Imgproc.findContours(imagenConBordes, listaContornos, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Convertir contornos a rectángulos
            for (MatOfPoint contorno : listaContornos) {
                Rect rectanguloContorno = Imgproc.boundingRect(contorno);
                listaRectangulosContornos.add(rectanguloContorno);
            }

            // Ordenar por área
            listaRectangulosContornos.sort((rect1, rect2) ->
                    Double.compare(rect2.area(), rect1.area()));

            double areaDeLaImagenCompleta = imagenConBordes.rows() * imagenConBordes.cols();
            double areaDeElPrimerRectangulo = listaRectangulosContornos.get(0).area();

            if (areaDeElPrimerRectangulo > (areaDeLaImagenCompleta / 2)) {
                for (int i = 1; i < 7 && i < listaRectangulosContornos.size(); i++) {
                    listaRectangulos.add(listaRectangulosContornos.get(i));
                }
            } else {
                for (int i = 0; i < 6 && i < listaRectangulosContornos.size(); i++) {
                    listaRectangulos.add(listaRectangulosContornos.get(i));
                }
            }

            // Ordenar subconjunto por coordenada X
            if (listaRectangulos.size() >= 5) {
                listaRectangulos.subList(1, 5).sort((rect1, rect2) ->
                        Integer.compare(rect1.x, rect2.x));
            }

            return listaRectangulos;
        } catch (Exception e) {
            Log.e(TAG, "Error al detectar contornos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void procesarSeccionRespuestas(Mat imagen, Rect rectanguloPadre,
                                                 ArrayList<Character> respuestasCorrectas, ArrayList<Character> respuestasAlumno, int seccion) {
        try {
            Mat imagenRecortada = imagen.submat(rectanguloPadre);
            int alturaSubRectangulo = rectanguloPadre.height / 10;
            int anchoSubRectangulo = rectanguloPadre.width;
            int indiceBase = (seccion - 1) * 10;

            for (int i = 0; i < 10; i++) {
                Rect subRectangulo = new Rect(
                        0,
                        i * alturaSubRectangulo,
                        anchoSubRectangulo,
                        alturaSubRectangulo
                );

                int indiceActual = indiceBase + i;
                Scalar color;

                // Determinar color basado en la comparación
                if (indiceActual < respuestasAlumno.size() && indiceActual < respuestasCorrectas.size()) {
                    if (respuestasAlumno.get(indiceActual) == 'X') {
                        color = new Scalar(0, 255, 255); // Amarillo
                    } else if (respuestasAlumno.get(indiceActual)
                            .equals(respuestasCorrectas.get(indiceActual))) {
                        color = new Scalar(0, 255, 0);   // Verde
                    } else {
                        color = new Scalar(0, 0, 255);   // Rojo
                    }
                } else {
                    color = new Scalar(0, 255, 255);     // Amarillo por defecto
                }

                // Dibujar rectángulo
                Imgproc.rectangle(imagenRecortada, subRectangulo.tl(),
                        subRectangulo.br(), color, 2);

                // Procesar subimagen
                Mat subimagenRecortada = imagenRecortada.submat(subRectangulo);
                Mat subimagenRecortadaGris = OpenCVUtil.setConvertirEscalaGrises(subimagenRecortada);
                Mat subimagenBinarizada = OpenCVUtil.setBinarizarImagen(subimagenRecortadaGris);

                detectarCirculos(subimagenBinarizada, imagenRecortada, subRectangulo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar sección de respuestas: " + e.getMessage());
        }
    }

    public static void procesarSeccionIdentificador(Mat imagenOriginal, Rect rectanguloPrincipal) {
        try {
            Mat imagenRecortada = imagenOriginal.submat(rectanguloPrincipal);
            Mat imagenEnEscalaGris = OpenCVUtil.setConvertirEscalaGrises(imagenRecortada);
            Mat imagenBinarizada = OpenCVUtil.setBinarizarImagen(imagenEnEscalaGris);

            List<Rect> listaDeSubRectangulos = detectarSubrectangulos(imagenBinarizada, true);

            // Procesar primer subrectángulo
            if (!listaDeSubRectangulos.isEmpty()) {
                Rect subRectangulo = listaDeSubRectangulos.get(0);
                Imgproc.rectangle(imagenRecortada,
                        subRectangulo.tl(),
                        subRectangulo.br(),
                        new Scalar(0, 255, 0), 2);

                Mat subImagenRecortada = imagenRecortada.submat(subRectangulo);
                Mat subImagenEscalaGrises = OpenCVUtil.setConvertirEscalaGrises(subImagenRecortada);
                detectarCirculosIdentificador(subImagenEscalaGrises,
                        imagenRecortada, subRectangulo, false, false);
            }

            // Procesar subrectángulos restantes
            for (int j = 1; j < Math.min(3, listaDeSubRectangulos.size()); j++) {
                Rect subRectangulo = listaDeSubRectangulos.get(j);
                Imgproc.rectangle(imagenRecortada,
                        subRectangulo.tl(),
                        subRectangulo.br(),
                        new Scalar(0, 255, 0), 2);

                Mat subImagenRecortada = imagenRecortada.submat(subRectangulo);
                Mat subImagenEscalaGrises = OpenCVUtil.setConvertirEscalaGrises(subImagenRecortada);
                detectarCirculosIdentificador(subImagenEscalaGrises,
                        imagenRecortada, subRectangulo, true, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar sección identificador: " + e.getMessage());
        }
    }

    public static void procesarSeccionNumeroExamen(Mat imagen, Rect rectangulo) {
        try {
            Mat imagenRecortada = imagen.submat(rectangulo);
            Mat imagenProcesada = procesarImagen(imagenRecortada);

            List<Rect> subRectangulos = detectarSubrectangulos(imagenProcesada, true);

            if (!subRectangulos.isEmpty()) {
                Rect rectNumeroExamen = subRectangulos.get(0);

                // Dibujar rectángulo verde alrededor del área detectada
                Imgproc.rectangle(imagenRecortada,
                        rectNumeroExamen.tl(),
                        rectNumeroExamen.br(),
                        new Scalar(0, 255, 0), 2);

                Mat subimagenRecortada = imagenRecortada.submat(rectNumeroExamen);
                Mat subimagenEscalaGrises = OpenCVUtil.setConvertirEscalaGrises(subimagenRecortada);

                // Detectar y procesar círculos para el número de examen
                detectarCirculosIdentificador(subimagenEscalaGrises,
                        imagenRecortada,
                        rectNumeroExamen,
                        false,
                        true);
            } else {
                Log.w(TAG, "No se detectaron subrectángulos en la sección de número de examen");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar sección número de examen: " + e.getMessage());
        }
    }

    private static List<Rect> detectarSubrectangulos(Mat bordesSubimagen, Boolean esIdentificador) {
        try {
            List<MatOfPoint> contornosSubimagen = new ArrayList<>();
            List<Rect> rectangulos = new ArrayList<>();

            // Encontrar contornos en la imagen
            Imgproc.findContours(bordesSubimagen,
                    contornosSubimagen,
                    new Mat(),
                    Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_SIMPLE);

            // Procesar cada contorno encontrado
            for (MatOfPoint contorno : contornosSubimagen) {
                Rect rect = Imgproc.boundingRect(contorno);

                // Filtrar rectángulos pequeños
                if (rect.width > 5 && rect.height > 5) {
                    boolean estaEnMismaPosicion = false;

                    // Verificar si ya existe un rectángulo en la misma posición
                    for (Rect rectanguloExistente : rectangulos) {
                        if (rectanguloExistente.tl().x == rect.tl().x &&
                                rectanguloExistente.tl().y == rect.tl().y) {
                            estaEnMismaPosicion = true;
                            break;
                        }
                    }

                    if (!estaEnMismaPosicion) {
                        rectangulos.add(rect);
                    }
                }
            }

            // Ordenar los rectángulos
            rectangulos.sort((rect1, rect2) ->
                    compararRectangulos(rect1, rect2, esIdentificador));

            return rectangulos;
        } catch (Exception e) {
            Log.e(TAG, "Error al detectar subrectángulos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static void detectarCirculos(Mat imagenGris, Mat imagenColor, Rect rect) {
            try {
                Mat imagenBinarizada = OpenCVUtil.setBinarizarImagen(imagenGris);
                Mat circulos = new Mat();

                // Detectar círculos usando HoughCircles
                Imgproc.HoughCircles(
                        imagenBinarizada,
                        circulos,
                        Imgproc.HOUGH_GRADIENT,
                        1,
                        imagenGris.cols() / 15,  // Distancia mínima entre círculos
                        100,                      // Umbral superior para Canny
                        9,                        // Umbral para la detección
                        6,                        // Radio mínimo
                        10                        // Radio máximo
                );

                List<Circulos> circulosDetectados = new ArrayList<>();

                // Procesar los primeros 4 círculos detectados
                for (int i = 0; i < Math.min(4, circulos.cols()); i++) {
                    double[] circ = circulos.get(0, i);
                    if (circ != null) {
                        Point centro = new Point(circ[0] + rect.x, circ[1] + rect.y);
                        int radio = (int) Math.round(circ[2]);
                        circulosDetectados.add(new Circulos(centro, radio));
                    } else {
                        Log.e(TAG, "Error al procesar las opciones. Se requiere una imagen más clara.");
                        return;
                    }
                }

                // Ordenar círculos por coordenada X
                circulosDetectados.sort((c1, c2) ->
                        Double.compare(c1.getCentro().x, c2.getCentro().x));

                ArrayList<Integer> pixelesPorCirculo = new ArrayList<>();

                // Procesar cada círculo detectado
                for (Circulos circulo : circulosDetectados) {
                    // Dibujar el círculo en la imagen
                    Imgproc.circle(imagenColor,
                            circulo.getCentro(),
                            circulo.getRadio(),
                            new Scalar(0, 0, 0),
                            1);

                    // Calcular el rectángulo que contiene el círculo
                    Rect rectCirculo = new Rect(
                            (int) (circulo.getCentro().x - circulo.getRadio()),
                            (int) (circulo.getCentro().y - circulo.getRadio()),
                            circulo.getRadio() * 2,
                            circulo.getRadio() * 2
                    );

                    // Ajustar el rectángulo a los límites de la imagen
                    rectCirculo = new Rect(
                            Math.max(0, rectCirculo.x),
                            Math.max(0, rectCirculo.y),
                            Math.min(imagenColor.cols() - rectCirculo.x, rectCirculo.width),
                            Math.min(imagenColor.rows() - rectCirculo.y, rectCirculo.height)
                    );

                    // Procesar y contar píxeles negros
                    int pixelesNegros = OpenCVUtil.procesarCirculos(imagenColor, rectCirculo);
                    pixelesPorCirculo.add(pixelesNegros);
                }

                // Añadir el grupo si tenemos 4 círculos
                if (pixelesPorCirculo.size() == 4) {
                    listaDePixelesPorGrupos.add(new ArrayList<>(pixelesPorCirculo));
                } else {
                    Log.e(TAG, "No se detectaron los 4 puntos necesarios. Verifica la claridad de la imagen.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en detectarCirculos: " + e.getMessage());
            }
        }

        private static void detectarCirculosIdentificador(Mat imagenGris, Mat imagenColor,
                                                          Rect rect, Boolean sonLasLetrasIdentificador, Boolean esElNumeroExamen) {
            try {
                ArrayList<Integer> listaTemporal = new ArrayList<>();
                Mat circulos = new Mat();

                // Detectar círculos
                Imgproc.HoughCircles(
                        imagenGris,
                        circulos,
                        Imgproc.HOUGH_GRADIENT,
                        1,
                        imagenGris.cols() / 15,
                        100,
                        12,
                        6,
                        11
                );

                List<Circulos> todosLosCirculos = new ArrayList<>();

                // Recolectar todos los círculos detectados
                for (int i = 0; i < circulos.cols(); i++) {
                    double[] circ = circulos.get(0, i);
                    if (circ != null) {
                        Point centro = new Point(circ[0] + rect.x, circ[1] + rect.y);
                        int radio = (int) Math.round(circ[2]);
                        todosLosCirculos.add(new Circulos(centro, radio));
                    } else {
                        Log.e(TAG, "Error al procesar las opciones. Se requiere una imagen más clara.");
                        return;
                    }
                }

                // Ordenar círculos según el tipo de procesamiento
                if (sonLasLetrasIdentificador) {
                    // Para letras: ordenar primero por Y, luego por X
                    todosLosCirculos.sort((c1, c2) -> {
                        int comparacionY = Double.compare(c1.getCentro().y, c2.getCentro().y);
                        return (comparacionY != 0) ?
                                comparacionY :
                                Double.compare(c1.getCentro().x, c2.getCentro().x);
                    });

                    // Procesar letras identificadoras
                    for (Circulos circulo : todosLosCirculos) {
                        Imgproc.circle(imagenColor,
                                circulo.getCentro(),
                                circulo.getRadio(),
                                new Scalar(0, 0, 0),
                                1);

                        Rect rectCirculo = new Rect(
                                (int) (circulo.getCentro().x - circulo.getRadio()),
                                (int) (circulo.getCentro().y - circulo.getRadio()),
                                circulo.getRadio() * 2,
                                circulo.getRadio() * 2
                        );

                        rectCirculo = new Rect(
                                Math.max(0, rectCirculo.x),
                                Math.max(0, rectCirculo.y),
                                Math.min(imagenColor.cols() - rectCirculo.x, rectCirculo.width),
                                Math.min(imagenColor.rows() - rectCirculo.y, rectCirculo.height)
                        );

                        if (rectCirculo.width > 0 && rectCirculo.height > 0) {
                            int pixelesNegros = OpenCVUtil.procesarCirculos(imagenColor, rectCirculo);
                            listaPixelesLetrasIdentificador.add(pixelesNegros);
                        }
                    }

                } else {
                    // Para números: ordenar solo por X
                    todosLosCirculos.sort((c1, c2) ->
                            Double.compare(c1.getCentro().x, c2.getCentro().x));

                    // Agrupar círculos en grupos de 10
                    List<List<Circulos>> gruposDeCirculos = new ArrayList<>();
                    for (int i = 0; i < todosLosCirculos.size(); i += 10) {
                        List<Circulos> grupo = new ArrayList<>();
                        for (int j = i; j < Math.min(i + 10, todosLosCirculos.size()); j++) {
                            grupo.add(todosLosCirculos.get(j));
                        }
                        grupo.sort((c1, c2) ->
                                Double.compare(c1.getCentro().y, c2.getCentro().y));
                        gruposDeCirculos.add(grupo);
                    }

                    // Procesar cada grupo
                    for (List<Circulos> grupo : gruposDeCirculos) {
                        procesarGrupoDeCirculos(grupo, imagenColor, listaTemporal, esElNumeroExamen);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en detectarCirculosIdentificador: " + e.getMessage());
            }
        }

        private static void procesarGrupoDeCirculos(List<Circulos> grupo, Mat imagenColor,
                                                    ArrayList<Integer> listaTemporal, boolean esElNumeroExamen) {
            for (Circulos circulo : grupo) {
                // Dibujar círculo
                Imgproc.circle(imagenColor,
                        circulo.getCentro(),
                        circulo.getRadio(),
                        new Scalar(0, 0, 255),
                        2);

                // Crear y ajustar rectángulo
                Rect rectCirculo = new Rect(
                        (int) (circulo.getCentro().x - circulo.getRadio()),
                        (int) (circulo.getCentro().y - circulo.getRadio()),
                        circulo.getRadio() * 2,
                        circulo.getRadio() * 2
                );

                rectCirculo = new Rect(
                        Math.max(0, rectCirculo.x),
                        Math.max(0, rectCirculo.y),
                        Math.min(imagenColor.cols() - rectCirculo.x, rectCirculo.width),
                        Math.min(imagenColor.rows() - rectCirculo.y, rectCirculo.height)
                );

                if (rectCirculo.width > 0 && rectCirculo.height > 0) {
                    int pixelesNegros = OpenCVUtil.procesarCirculos(imagenColor, rectCirculo);
                    listaTemporal.add(pixelesNegros);

                    if (listaTemporal.size() == 10) {
                        if (esElNumeroExamen) {
                            listaDePixelesPorGruposNumeroExamen.add(
                                    new ArrayList<>(listaTemporal));
                        } else {
                            listaDePixelesPorGruposIdentificador.add(
                                    new ArrayList<>(listaTemporal));
                        }
                        listaTemporal.clear();
                    }
                }
            }
        }

    private static int compararRectangulos(Rect rect1, Rect rect2, Boolean esIdentificador) {
        try {
            boolean rect1CumpleCriterio;
            boolean rect2CumpleCriterio;

            if (esIdentificador) {
                rect1CumpleCriterio = rect1.width < rect1.height;
                rect2CumpleCriterio = rect2.width < rect2.height;
            } else {
                rect1CumpleCriterio = rect1.width > rect1.height;
                rect2CumpleCriterio = rect2.width > rect2.height;
            }

            if (rect1CumpleCriterio && !rect2CumpleCriterio) {
                return -1;
            } else if (!rect1CumpleCriterio && rect2CumpleCriterio) {
                return 1;
            }

            double area1 = rect1.area();
            double area2 = rect2.area();

            int compruebaArea = Double.compare(area2, area1);
            if (compruebaArea != 0) {
                return compruebaArea;
            }

            int compararCordenadaY = Double.compare(rect1.tl().y, rect2.tl().y);
            if (compararCordenadaY != 0) {
                return compararCordenadaY;
            }

            return Double.compare(rect2.tl().x, rect1.tl().x);
        } catch (Exception e) {
            Log.e(TAG, "Error en compararRectangulos: " + e.getMessage());
            return 0;
        }
    }
}
