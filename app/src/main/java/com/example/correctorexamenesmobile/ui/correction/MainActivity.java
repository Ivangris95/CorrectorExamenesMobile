package com.example.correctorexamenesmobile.ui.correction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.correctorexamenesmobile.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    // Variables para la plantilla (primer botón)
    private Uri plantillaUri;
    private String plantillaPhotoPath;

    // Variables para el examen (segundo botón)
    private Uri examenUri;
    private String examenPhotoPath;

    // Variable para saber qué imagen estamos procesando
    private boolean isPlantilla = true;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        // Guardar URI y path según el botón que se presionó
                        if (isPlantilla) {
                            plantillaUri = imageUri;
                            plantillaPhotoPath = getRealPathFromUri(plantillaUri);
                            Log.i("RUTA_IMAGEN", "Ruta de plantilla: " + plantillaPhotoPath);
                            Log.i("RUTA_IMAGEN", "Uri de plantilla: " + plantillaUri.toString());
                            Toast.makeText(this, "Plantilla guardada: " + plantillaPhotoPath, Toast.LENGTH_SHORT).show();
                        } else {
                            examenUri = imageUri;
                            examenPhotoPath = getRealPathFromUri(examenUri);
                            Log.i("RUTA_IMAGEN", "Ruta de examen: " + examenPhotoPath);
                            Log.i("RUTA_IMAGEN", "Uri de examen: " + examenUri.toString());
                            Toast.makeText(this, "Examen guardado: " + examenPhotoPath, Toast.LENGTH_SHORT).show();
                        }

                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        int targetW = binding.imgView.getWidth();
                        int targetH = binding.imgView.getHeight();

                        if (targetW > 0 && targetH > 0) {
                            Bitmap resizedBitmap = resizeBitmap(bitmap, targetW, targetH);
                            binding.imgView.setImageBitmap(resizedBitmap);
                        } else {
                            binding.imgView.post(() -> {
                                int width = binding.imgView.getWidth();
                                int height = binding.imgView.getHeight();
                                Bitmap resizedBitmap = resizeBitmap(bitmap, width, height);
                                binding.imgView.setImageBitmap(resizedBitmap);
                            });
                        }
                        binding.imgView.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al cargar la imagen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Launcher para permisos de cámara
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    abrirCamara();
                } else {
                    Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher para la cámara
    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        try {
                            // Guardar la imagen y obtener su path
                            File photoFile = createImageFile();
                            if (photoFile != null) {
                                FileOutputStream fos = new FileOutputStream(photoFile);
                                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.close();

                                // Guardar path según el botón que se presionó
                                if (isPlantilla) {
                                    plantillaPhotoPath = photoFile.getAbsolutePath();
                                    plantillaUri = Uri.fromFile(photoFile);
                                    Log.i("RUTA_IMAGEN", "Ruta de plantilla (cámara): " + plantillaPhotoPath);
                                    Log.i("URI_IMAGEN", "URI de plantilla (cámara): " + plantillaUri.toString());
                                    Toast.makeText(this, "Plantilla guardada: " + plantillaPhotoPath, Toast.LENGTH_SHORT).show();
                                } else {
                                    examenPhotoPath = photoFile.getAbsolutePath();
                                    examenUri = Uri.fromFile(photoFile);
                                    Log.i("RUTA_IMAGEN", "Ruta de examen (cámara): " + examenPhotoPath);
                                    Log.i("URI_IMAGEN", "URI de examen (cámara): " + examenUri.toString());
                                    Toast.makeText(this, "Examen guardado: " + examenPhotoPath, Toast.LENGTH_SHORT).show();
                                }
                            }

                            int targetW = binding.imgView.getWidth();
                            int targetH = binding.imgView.getHeight();

                            if (targetW > 0 && targetH > 0) {
                                Bitmap resizedBitmap = resizeBitmap(imageBitmap, targetW, targetH);
                                binding.imgView.setImageBitmap(resizedBitmap);
                            } else {
                                binding.imgView.post(() -> {
                                    int width = binding.imgView.getWidth();
                                    int height = binding.imgView.getHeight();
                                    Bitmap resizedBitmap = resizeBitmap(imageBitmap, width, height);
                                    binding.imgView.setImageBitmap(resizedBitmap);
                                });
                            }
                            binding.imgView.setVisibility(View.VISIBLE);
                        } catch (IOException e) {
                            Toast.makeText(this, "Error al guardar la imagen: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.platillaButton.setOnClickListener(v -> {
            // Restaurar botón de examen si está dividido
            restaurarBotonExamen();

            // Ocultar botón de plantilla y mostrar sus botones
            binding.platillaButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        binding.platillaButton.setVisibility(View.GONE);

                        // Mostrar nuevos botones
                        binding.fotoButton.setVisibility(View.VISIBLE);
                        binding.galeriaButton.setVisibility(View.VISIBLE);

                        binding.galeriaButton.animate()
                                .translationX(-100f)
                                .alpha(1f)
                                .setDuration(300);

                        binding.fotoButton.animate()
                                .translationX(100f)
                                .alpha(1f)
                                .setDuration(300);
                    });
        });

        binding.examenButton.setOnClickListener(v -> {
            // Restaurar botón de plantilla si está dividido
            restaurarBotonPlantilla();

            // Ocultar botón de examen y mostrar sus botones
            binding.examenButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        binding.examenButton.setVisibility(View.GONE);

                        // Mostrar nuevos botones
                        binding.fotoButton2.setVisibility(View.VISIBLE);
                        binding.galeriaButton2.setVisibility(View.VISIBLE);

                        binding.galeriaButton2.animate()
                                .translationX(-100f)
                                .alpha(1f)
                                .setDuration(300);

                        binding.fotoButton2.animate()
                                .translationX(100f)
                                .alpha(1f)
                                .setDuration(300);
                    });
        });

        binding.fotoButton.setOnClickListener(v -> {
            isPlantilla = true;
            verificarPermisosYAbrirCamara();
        });

        binding.galeriaButton.setOnClickListener(v -> {
            isPlantilla = true;
            abrirGaleria();
        });

        binding.fotoButton2.setOnClickListener(v -> {
            isPlantilla = false;
            verificarPermisosYAbrirCamara();
        });

        binding.galeriaButton2.setOnClickListener(v -> {
            isPlantilla = false;
            abrirGaleria();
        });
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        try {
            pickImageLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir la galería: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Método para redimensionar la imagen manteniendo la relación de aspecto
    private Bitmap resizeBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        float scaleFactor = Math.min(
                targetWidth / (float) bitmap.getWidth(),
                targetHeight / (float) bitmap.getHeight()
        );

        int finalWidth = Math.round(bitmap.getWidth() * scaleFactor);
        int finalHeight = Math.round(bitmap.getHeight() * scaleFactor);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            takePictureLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir la cámara: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarPermisosYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    // Método para restaurar el botón de plantilla
    private void restaurarBotonPlantilla() {
        if (binding.fotoButton.getVisibility() == View.VISIBLE ||
                binding.galeriaButton.getVisibility() == View.VISIBLE) {

            // Ocultar botones divididos instantáneamente
            binding.fotoButton.setVisibility(View.GONE);
            binding.galeriaButton.setVisibility(View.GONE);

            // Restaurar posiciones originales
            binding.fotoButton.setTranslationX(0f);
            binding.galeriaButton.setTranslationX(0f);

            // Mostrar botón principal instantáneamente
            binding.platillaButton.setVisibility(View.VISIBLE);
            binding.platillaButton.setAlpha(1f);
        }
    }

    // Método para restaurar el botón de examen
    private void restaurarBotonExamen() {
        if (binding.fotoButton2.getVisibility() == View.VISIBLE ||
                binding.galeriaButton2.getVisibility() == View.VISIBLE) {

            // Ocultar botones divididos instantáneamente
            binding.fotoButton2.setVisibility(View.GONE);
            binding.galeriaButton2.setVisibility(View.GONE);

            // Restaurar posiciones originales
            binding.fotoButton2.setTranslationX(0f);
            binding.galeriaButton2.setTranslationX(0f);

            // Mostrar botón principal instantáneamente
            binding.examenButton.setVisibility(View.VISIBLE);
            binding.examenButton.setAlpha(1f);
        }
    }

}
