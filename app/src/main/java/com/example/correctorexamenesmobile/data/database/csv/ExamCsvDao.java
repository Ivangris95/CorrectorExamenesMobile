package com.example.correctorexamenesmobile.data.database.csv;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ExamCsvDao {
    private static final String CSV_FILENAME = "plantilla.csv";
    private static final String SEPARATOR = ";";
    private static final String NEW_LINE = "\n";
    private final Context context;

    public ExamCsvDao(Context context) {
        this.context = context;
    }

    // Obtener el directorio de archivos internos de la aplicación
    private File getFile() {
        File directory = context.getFilesDir();
        return new File(directory,CSV_FILENAME);
    }

    public boolean existeExamen(String codigoExamen) {
        File archivo = getFile();

        if(!archivo.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))){
            String linea;

            // Saltamos la primera línea (encabezados)
            br.readLine();

            while ((linea = br.readLine()) != null) {

                // Ignorar líneas vacías y líneas con solo un separador
                String[] datos = linea.split(SEPARATOR);

                if (datos.length > 0 && datos[0].equals(codigoExamen)) return true;
            }
            return false;

        } catch (IOException e) {
            Log.e("ExamCsvDao", "Error al leer el archivo", e);
            return false;
        }
    }

    public void guardarResultados(String codigoExamen, ArrayList<Character> resultados) {

        if (existeExamen(codigoExamen)) { Log.i("ExamCsvDao", "El examen ya existe en el sistema");return;}

        File archivo = getFile();
        boolean existeArchivo = archivo.exists();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo, true))) {
            if (!existeArchivo) {
                bw.write("Codigo_Examen,Resultados");
                bw.write(NEW_LINE);
            }

            // Preparar la línea de resultados
            StringBuilder linea = new StringBuilder();
            linea.append(codigoExamen).append(SEPARATOR);

            // Convertir el ArrayList<Character> a String
            for (Character resultado : resultados) {
                linea.append(resultado);
            }

            // Escribir la línea en el archivo
            bw.write(linea.toString());
            bw.write(NEW_LINE);

            Log.i("ExamCsvDao", "Resultados guardados exitosamente");

        } catch (IOException e) {
            Log.e("ExamCsvDao", "Error al guardar resultados", e);
        }
    }

    public String obtenerRespuestas(String codigoExamen) {
        File archivo = getFile();

        if (!archivo.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            // Leer la primera línea (encabezados)
            br.readLine();

            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(SEPARATOR);

                // Encontrar la línea con el código de examen
                if (datos.length > 1 && datos[0].trim().equals(codigoExamen.trim())) return datos[1].trim();
            }
        } catch (IOException e) {
            Log.e("ExamCsvDao", "Error al leer respuestas", e);
        }
        return null;
    }

    public void inicializarArchivo() {
        File archivo = getFile();

        if (!archivo.exists()) {

            // Crear el archivo CSV inicialmente con los encabezados
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
                bw.write("Codigo_Examen,Resultados");
                bw.write(NEW_LINE);
                Log.i("ExamCsvDao", "Archivo CSV inicializado correctamente");

            } catch (IOException e) {
                Log.e("ExamCsvDao", "Error al inicializar archivo CSV", e);
            }
        }
    }

}
