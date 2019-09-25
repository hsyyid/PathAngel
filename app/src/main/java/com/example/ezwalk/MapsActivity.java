package com.example.ezwalk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.Lists;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    PlacesClient placesClient;
    LatLng startingPoint;
    LatLng destinationPoint;
    FloatingActionButton fab;
    RequestQueue volleyHandler;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fab = findViewById(R.id.fab);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), System.getenv("API_KEY"));
        }

        placesClient = Places.createClient(this);
        volleyHandler = Volley.newRequestQueue(this);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteSupportFragment.a.setTextColor(Color.WHITE);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                startingPoint = place.getLatLng();

                mMap.clear();
                mMap.addCircle(
                        new CircleOptions()
                                .center(startingPoint)
                                .radius(20.0)
                                .strokeWidth(3f)
                                .strokeColor(Color.RED)
                                .fillColor(Color.argb(70, 150, 50, 50)));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 17));
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        final AutocompleteSupportFragment autocompleteSupportFragment2 = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment2);
        autocompleteSupportFragment2.a.setTextColor(Color.WHITE);

        autocompleteSupportFragment2.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        autocompleteSupportFragment2.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destinationPoint = place.getLatLng();

                mMap.addMarker(new MarkerOptions().position(destinationPoint).title("Your Location"));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(startingPoint);
                builder.include(destinationPoint);

                final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 75);
                mMap.animateCamera(cu);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        autocompleteSupportFragment.setHint("Enter Starting Point..");
        autocompleteSupportFragment2.setHint("Enter Destination..");

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startingPoint == null || destinationPoint == null) {
                    Toast.makeText(MapsActivity.this, "Oops. Did you forget to enter the start and end points?!", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                final ProgressBar progressBar = findViewById(R.id.progress);
                progressBar.setVisibility(View.VISIBLE);
                String url = "http://10.128.25.174:5000/find_path?location=" + startingPoint.latitude + "," + startingPoint.longitude + "&destination=" + destinationPoint.latitude + "," + destinationPoint.longitude;
                mMap.clear();

                JsonObjectRequest request = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    // Get the path
                                    String path = response.getString("best_path");
                                    // Decode
                                    List<LatLng> polylinePath = PolyUtil.decode(path);

                                    // Put it on the map
                                    mMap.addMarker(new MarkerOptions().position(destinationPoint).title("Your Location"));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 17f));
                                    mMap.addPolyline(
                                            new PolylineOptions()
                                                    .addAll(polylinePath)
                                                    .width(8f)
                                                    .color(Color.RED));
                                    mMap.addCircle(
                                            new CircleOptions()
                                                    .center(startingPoint)
                                                    .radius(80.0)
                                                    .strokeWidth(3f)
                                                    .strokeColor(Color.argb(100, 241, 160, 43))
                                                    .fillColor(Color.argb(70, 150, 50, 50)));
                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    builder.include(startingPoint);
                                    builder.include(destinationPoint);

                                    final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 75);
                                    mMap.animateCamera(cu);
                                    progressBar.setVisibility(View.GONE);

                                    // Convert all data from JSON -> LatLng list
                                    JSONArray heatmapData = response.getJSONArray("heatmap");
                                    System.out.println(heatmapData.toString());
                                    List<LatLng> hmList = Lists.newArrayList();

                                    for (int i = 0; i < heatmapData.length(); i++) {
                                        JSONArray heatmapEntry = heatmapData.getJSONArray(i);
                                        double lat = heatmapEntry.getDouble(0);
                                        double lng = heatmapEntry.getDouble(1);
                                        hmList.add(new LatLng(lat, lng));
                                    }

                                    // Add to heat map
                                    if (!hmList.isEmpty())
                                        addHeatMap(hmList);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO: Handle error
                                error.printStackTrace();
                            }
                        });

                request.setRetryPolicy(new DefaultRetryPolicy(
                        450000,
                        0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                volleyHandler.add(request);
            }
        });

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                mMap.clear();
                startingPoint = lastKnownLocation != null
                        ? new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())
                        : new LatLng(42.45008, -76.4815593);
                mMap.addCircle(
                        new CircleOptions()
                                .center(startingPoint)
                                .radius(20.0)
                                .strokeWidth(3f)
                                .strokeColor(Color.RED)
                                .fillColor(Color.argb(70, 150, 50, 50)));

                mMap.moveCamera(CameraUpdateFactory.newLatLng(startingPoint));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, 17));
            }
        }
    }


    private void addHeatMap(List<LatLng> list) {
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(list).build();
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.profile:
                            Intent a = new Intent(getApplicationContext(), ProfileActivity.class);
                            startActivity(a);
                            break;

                        case R.id.settings:
                            Intent b = new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(b);
                            break;
                    }
                    return false;
                }
            };
}
