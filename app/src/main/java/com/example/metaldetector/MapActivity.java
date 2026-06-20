package com.example.metaldetector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends Activity {
    private static final int REQUEST_LOCATION = 77;
    private static final String PREFS = "detector_settings";

    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline trackPolyline;
    private Handler trackHandler;
    private Runnable trackRunnable;
    private NotesDatabase notesDb;

    private TextView statusText;
    private Button recordButton;
    private Button stopButton;
    private Button addNoteButton;
    private Button refreshButton;
    private Button backButton;

    private volatile GeoPoint lastKnownLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences(PREFS, MODE_PRIVATE));
        notesDb = new NotesDatabase(this);

        setContentView(createLayout());

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        setupLocationOverlay();
        setupTrackPolyline();
        loadNotesMarkers();
        startTrackUpdater();

        if (!hasLocationPermission()) {
            requestLocationPermission();
        }
    }

    private View createLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);
        root.setBackgroundColor(Color.rgb(246, 248, 251));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset = insets.getSystemWindowInsetBottom();
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            return insets;
        });

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        topBar.setBackgroundColor(Color.rgb(14, 17, 22));
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        backButton = new Button(this);
        backButton.setText("← Назад");
        backButton.setAllCaps(false);
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(v -> finish());
        topBar.addView(backButton, new LinearLayout.LayoutParams(-2, -2));

        statusText = new TextView(this);
        statusText.setText("Карта");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, -2, 1f);
        topBar.addView(statusText, statusParams);

        root.addView(topBar, new LinearLayout.LayoutParams(-1, -2));

        // Map view
        mapView = new MapView(this);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
        root.addView(mapView, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Bottom controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(dp(8), dp(8), dp(8), dp(8));
        controls.setGravity(Gravity.CENTER);
        controls.setBackgroundColor(Color.rgb(246, 248, 251));

        recordButton = new Button(this);
        recordButton.setText("▶ Запись");
        recordButton.setAllCaps(false);
        recordButton.setOnClickListener(v -> startRecording());
        controls.addView(recordButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        stopButton = new Button(this);
        stopButton.setText("⏹ Стоп");
        stopButton.setAllCaps(false);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> stopRecording());
        controls.addView(stopButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        addNoteButton = new Button(this);
        addNoteButton.setText("📝 Заметка");
        addNoteButton.setAllCaps(false);
        addNoteButton.setOnClickListener(v -> showAddNoteDialog());
        controls.addView(addNoteButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        refreshButton = new Button(this);
        refreshButton.setText("🔄 Обновить");
        refreshButton.setAllCaps(false);
        refreshButton.setOnClickListener(v -> loadNotesMarkers());
        controls.addView(refreshButton, new LinearLayout.LayoutParams(0, dp(48), 1f));

        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        return root;
    }

    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void setupTrackPolyline() {
        trackPolyline = new Polyline();
        trackPolyline.setColor(Color.rgb(15, 118, 110));
        trackPolyline.setWidth(6f);
        mapView.getOverlays().add(trackPolyline);
    }

    private void startTrackUpdater() {
        trackHandler = new Handler(Looper.getMainLooper());
        trackRunnable = new Runnable() {
            @Override
            public void run() {
                if (TrackRecorder.isRunning && !TrackRecorder.trackPoints.isEmpty()) {
                    trackPolyline.setPoints(new ArrayList<>(TrackRecorder.trackPoints));
                    statusText.setText("Трек: " + TrackRecorder.trackPoints.size() + " точек");
                } else if (TrackRecorder.isRunning) {
                    statusText.setText("Ожидание GPS...");
                } else {
                    statusText.setText("Карта");
                }

                // Update last known location from overlay
                if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                    lastKnownLocation = myLocationOverlay.getMyLocation();
                }

                mapView.invalidate();
                trackHandler.postDelayed(this, 1000);
            }
        };
        trackHandler.post(trackRunnable);
    }

    private void startRecording() {
        if (!TrackRecorder.isRunning) {
            Intent intent = new Intent(this, TrackRecorder.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            recordButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusText.setText("Запись начата...");
        }
    }

    private void stopRecording() {
        if (TrackRecorder.isRunning) {
            Intent intent = new Intent(this, TrackRecorder.class);
            stopService(intent);
            recordButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusText.setText("Запись остановлена");
        }
    }

    private void showAddNoteDialog() {
        GeoPoint point = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            point = myLocationOverlay.getMyLocation();
        } else if (lastKnownLocation != null) {
            point = lastKnownLocation;
        } else {
            point = (GeoPoint) mapView.getMapCenter();
        }

        final GeoPoint notePoint = point;

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(16));
        scroll.addView(layout);

        TextView coordText = new TextView(this);
        coordText.setText(String.format("Координаты: %.5f, %.5f", notePoint.getLatitude(), notePoint.getLongitude()));
        coordText.setTextColor(Color.rgb(51, 65, 85));
        coordText.setPadding(0, 0, 0, dp(12));
        layout.addView(coordText);

        EditText titleInput = new EditText(this);
        titleInput.setHint("Заголовок заметки");
        layout.addView(titleInput);

        EditText descInput = new EditText(this);
        descInput.setHint("Описание");
        descInput.setMinLines(3);
        layout.addView(descInput);

        final GeoPoint finalPoint = notePoint;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить заметку")
                .setView(scroll)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String desc = descInput.getText().toString().trim();
                    if (title.isEmpty()) title = "Заметка";

                    Note note = new Note(title, desc, finalPoint.getLatitude(), finalPoint.getLongitude(), System.currentTimeMillis());
                    notesDb.addNote(note);
                    addMarker(note);
                    mapView.invalidate();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadNotesMarkers() {
        // Remove old note markers (keep location overlay and track polyline)
        List<org.osmdroid.views.overlay.Overlay> toKeep = new ArrayList<>();
        for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof MyLocationNewOverlay || overlay instanceof Polyline) {
                toKeep.add(overlay);
            }
        }
        mapView.getOverlays().clear();
        mapView.getOverlays().addAll(toKeep);

        List<Note> notes = notesDb.getAllNotes();
        for (Note note : notes) {
            addMarker(note);
        }
        mapView.invalidate();
    }

    private void addMarker(Note note) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(note.getLatitude(), note.getLongitude()));
        marker.setTitle(note.getTitle());
        marker.setSnippet(note.getDescription());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation, null));
        marker.setOnMarkerClickListener((m, mv) -> {
            showNoteDetails(note);
            return true;
        });
        mapView.getOverlays().add(marker);
    }

    private void showNoteDetails(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(note.getTitle())
                .setMessage(note.getDescription() + "\n\n" +
                        String.format("Координаты: %.5f, %.5f", note.getLatitude(), note.getLongitude()) + "\n" +
                        "Время: " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(note.getTimestamp())))
                .setPositiveButton("OK", null)
                .setNegativeButton("Удалить", (dialog, which) -> {
                    notesDb.deleteNote(note.getId());
                    loadNotesMarkers();
                })
                .show();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (myLocationOverlay != null) {
                    myLocationOverlay.enableMyLocation();
                }
            } else {
                statusText.setText("GPS разрешение отклонено");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trackHandler != null) {
            trackHandler.removeCallbacks(trackRunnable);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
