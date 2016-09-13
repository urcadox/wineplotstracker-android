package vitisoft.vitisoftapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import vitisoft.vitisoftapp.models.entities.Plot;
import vitisoft.vitisoftapp.views.RVAdapter;

public class PlotActivity extends AppCompatActivity implements OnMapReadyCallback {

    public GoogleMap map;
    public LinkedList<LatLng> polygonCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle("Parcelle");
        Intent intent = getIntent();
        String plotId = intent.getStringExtra(Consts.PLOT_ID);

        String url = "https://vitisoft.cleverapps.io/api/plots/" + plotId;
        new RetrievePlotTask().execute(url);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.plot_map);
        mapFragment.getMapAsync(this);
    }

    class RetrievePlotTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... url) {
            try {
                URL plotURL = new URL(url[0]);
                HttpsURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpsURLConnection) plotURL.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    InputStreamReader reader = new InputStreamReader(urlConnection.getInputStream());
                    String rawData = IOUtils.toString(reader);
                    IOUtils.closeQuietly(reader);
                    return rawData;
                }
                catch(IOException e) {
                    Log.e("vitisoft IO error", e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          Toast.makeText(getApplicationContext(), "Erreur réseau", Toast.LENGTH_LONG).show();
                      }
                     });
                    return null;
                } finally {
                    if(urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } catch(MalformedURLException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      Toast.makeText(getApplicationContext(), "URL invalide", Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
        }

        protected void onPostExecute(String rawData) {
            if(rawData != null) {
                Gson gson = new Gson();
                Type plotType = new TypeToken<Plot>(){}.getType();
                Plot plot = gson.fromJson(rawData, plotType);

                getSupportActionBar().setTitle(plot.name);

                polygonCoordinates = new LinkedList();
                for(int i = 0; i < plot.position.length; i++) {
                    polygonCoordinates.add(new LatLng(plot.position[i][1], plot.position[i][0]));
                }

                showPolygonOnMap();

            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(43.273645, -0.406838), 11));
        showPolygonOnMap();
    }

    public void showPolygonOnMap() {
        if (map != null && polygonCoordinates != null) {
            Log.e("plop", "got map and polygon");
            PolygonOptions polygon = new PolygonOptions()
                .addAll(polygonCoordinates)
                .fillColor(Color.argb(20, 0, 170, 0))
                .strokeColor(Color.argb(80, 0, 255, 0))
                .strokeWidth(5);
            map.addPolygon(polygon);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latlng : polygonCoordinates) {
                builder.include(latlng);
            }
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300));
        }
    }

}