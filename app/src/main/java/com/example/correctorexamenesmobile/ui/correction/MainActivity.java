package com.example.correctorexamenesmobile.ui.correction;


import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.example.correctorexamenesmobile.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.platillaButton.setOnClickListener(v -> {
            //Ocultar boton
            binding.platillaButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        binding.platillaButton.setVisibility(View.GONE);

                        //Mostrar neuvos botones
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
            //Ocultar boton
            binding.examenButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        binding.examenButton.setVisibility(View.GONE);

                        //Mostrar neuvos botones
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

    }
}
