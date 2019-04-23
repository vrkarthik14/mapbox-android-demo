package com.mapbox.mapboxandroiddemo.examples.labs;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeta;
import com.mapbox.turf.TurfTransformation;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.turf.TurfConstants.UNIT_MILES;

/**
 * Use {@link TurfTransformation#circle(Point, double, int, String)} to draw a hollow circle
 * (i.e. ring) around a center coordinate.
 */
public class TurfRingActivity extends AppCompatActivity {
  private static final String OUTER_CIRCLE_GEOJSON_SOURCE_ID = "OUTER_CIRCLE_GEOJSON_SOURCE_ID";
  private static final String INNER_CIRCLE_GEOJSON_SOURCE_ID = "INNER_CIRCLE_GEOJSON_SOURCE_ID";
  private static final String OUTER_CIRCLE_LAYER_ID = "OUTER_CIRCLE_LAYER_ID";
  private static final String INNER_CIRCLE_LAYER_ID = "INNER_CIRCLE_LAYER_ID";
  private static final int OUTER_CIRCLE_MILE_RADIUS = 2;
  private static final double MILE_DIFFERENCE_BETWEEN_CIRCLES = .5;
  private static final int CIRCLE_STEPS = 360;
  private static final Point POINT_IN_MIDDLE_OF_CIRCLE = Point.fromLngLat(-115.150738, 36.16218);
  private MapView mapView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_lab_hollow_circle);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(@NonNull MapboxMap mapboxMap) {
        mapboxMap.setStyle(new Style.Builder()
          .fromUrl(Style.LIGHT)
          .withSource(new GeoJsonSource(OUTER_CIRCLE_GEOJSON_SOURCE_ID))
          .withSource(new GeoJsonSource(INNER_CIRCLE_GEOJSON_SOURCE_ID))
          .withLayer(new FillLayer(OUTER_CIRCLE_LAYER_ID, OUTER_CIRCLE_GEOJSON_SOURCE_ID).withProperties(
            fillColor(Color.RED)
          ))
          .withLayer(new FillLayer(INNER_CIRCLE_LAYER_ID, INNER_CIRCLE_GEOJSON_SOURCE_ID).withProperties(
            fillColor(Color.RED)
          )), new Style.OnStyleLoaded() {
          @Override
          public void onStyleLoaded(@NonNull Style style) {

                  // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
                  // Use Turf to calculate the coordinates for the outer ring of the final Polygon
                  Polygon outerCirclePolygon = getTurfPolygon(OUTER_CIRCLE_MILE_RADIUS);

                  // Use Turf to calculate the coordinates for the inner ring of the final Polygon
                  Polygon innerCirclePolygon = getTurfPolygon(OUTER_CIRCLE_MILE_RADIUS - MILE_DIFFERENCE_BETWEEN_CIRCLES);

                  GeoJsonSource outerCircleSource = style.getSourceAs(OUTER_CIRCLE_GEOJSON_SOURCE_ID);
                  if (outerCircleSource != null) {

                    // Use the two Polygon objects above to create the final Polygon that visually represents the ring.
                    outerCircleSource.setGeoJson(Polygon.fromOuterInner(
                      // Create outer LineString
                      LineString.fromLngLats(TurfMeta.coordAll(outerCirclePolygon, false)),
                      // Create inter LineString
                      LineString.fromLngLats(TurfMeta.coordAll(innerCirclePolygon, false))
                    ));

                  }
          }
        });
      }
    });
  }

  private Polygon getTurfPolygon(@NonNull double radius) {
    return TurfTransformation.circle(POINT_IN_MIDDLE_OF_CIRCLE, radius, CIRCLE_STEPS, UNIT_MILES);
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}
