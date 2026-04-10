package com.routinely.app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.routinely.app.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-screen barcode scan activity using CameraX + ML Kit.
 * Returns the scanned value via EXTRA_BARCODE_VALUE.
 * Optional: pass EXTRA_TARGET_VALUE to validate the scan against a known barcode.
 */
public class BarcodeScanActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE_VALUE = "barcode_value";
    public static final String EXTRA_TARGET_VALUE  = "target_value";
    public static final String EXTRA_PROMPT        = "prompt";

    private static final int REQUEST_CAMERA = 101;

    private PreviewView previewView;
    private TextView tvStatus;
    private Button btnTorch;
    private Camera camera;
    private boolean torchOn = false;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private String targetValue;
    private volatile boolean resultDelivered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);

        previewView = findViewById(R.id.camera_preview);
        tvStatus    = findViewById(R.id.tv_scan_status);
        btnTorch    = findViewById(R.id.btn_torch);

        targetValue = getIntent().getStringExtra(EXTRA_TARGET_VALUE);
        String prompt = getIntent().getStringExtra(EXTRA_PROMPT);
        if (prompt != null && !prompt.isEmpty()) {
            TextView tvInstr = findViewById(R.id.tv_scan_instruction);
            tvInstr.setText(prompt);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build();
        scanner = BarcodeScanning.getClient(options);

        btnTorch.setOnClickListener(v -> toggleTorch());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindCamera(provider);
            } catch (Exception e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider provider) {
        provider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
            .setTargetResolution(new Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
        analysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (resultDelivered) { imageProxy.close(); return; }
            @SuppressWarnings("UnsafeOptInUsageError")
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String raw = barcode.getRawValue();
                            if (raw == null || raw.isEmpty()) continue;
                            onBarcodeDetected(raw);
                            break;
                        }
                    })
                    .addOnFailureListener(e -> { /* ignore */ })
                    .addOnCompleteListener(t -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
        camera = provider.bindToLifecycle(this, selector, preview, analysis);
    }

    private void onBarcodeDetected(String value) {
        if (resultDelivered) return;
        // If there's a target value, check it matches
        if (targetValue != null && !targetValue.isEmpty()) {
            if (!value.equals(targetValue)) {
                runOnUiThread(() -> {
                    tvStatus.setText("Wrong barcode — scan the correct item");
                    tvStatus.setTextColor(0xFFEF4444);
                });
                return;
            }
        }
        resultDelivered = true;
        Intent result = new Intent();
        result.putExtra(EXTRA_BARCODE_VALUE, value);
        setResult(RESULT_OK, result);
        finish();
    }

    private void toggleTorch() {
        if (camera == null) return;
        torchOn = !torchOn;
        camera.getCameraControl().enableTorch(torchOn);
        btnTorch.setText(torchOn ? "🔦 Torch ON" : "🔦 Torch");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required for barcode scanning",
                Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        scanner.close();
        super.onDestroy();
    }
}
