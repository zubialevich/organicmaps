package app.organicmaps.location;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.organicmaps.MwmApplication;
import app.organicmaps.R;

class SensorHelper implements SensorEventListener
{
  @NonNull
  private final SensorManager mSensorManager;
  @Nullable
  private final Sensor mRotationVectorSensor;
  @NonNull
  private final MwmApplication mMwmApplication;

  private final float[] mRotationMatrix = new float[9];
  private final float[] mRotationValues = new float[3];

  @Override
  public void onSensorChanged(SensorEvent event)
  {
    // Here we can have events from one out of these two sensors:
    // TYPE_GEOMAGNETIC_ROTATION_VECTOR
    // TYPE_ROTATION_VECTOR
    SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mRotationMatrix);

    SensorManager.getOrientation(mRotationMatrix, mRotationValues);

    // mRotationValues indexes: 0 - yaw, 2 - roll, 1 - pitch.
    LocationHelper.INSTANCE.notifyCompassUpdated(mRotationValues[0]);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {
    // Here we can have events from one out of these two sensors:
    // TYPE_GEOMAGNETIC_ROTATION_VECTOR
    // TYPE_ROTATION_VECTOR
    Log.i("onAccuracyChanged", "Sensor " + sensor.getStringType() + " has changed accuracy to " + accuracy);
    String toastText = null;
    switch (accuracy)
    {
      case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
        break;
      case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
        toastText = mMwmApplication.getString(R.string.compass_calibration_recommended);
        break;
      case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
      case SensorManager.SENSOR_STATUS_UNRELIABLE:
      default:
        toastText = mMwmApplication.getString(R.string.compass_calibration_required);
    }
    if (toastText != null)
      Toast.makeText(mMwmApplication, toastText, Toast.LENGTH_LONG).show();
  }

  SensorHelper(@NonNull Context context)
  {
    mMwmApplication = MwmApplication.from(context);
    mSensorManager = (SensorManager) mMwmApplication.getSystemService(Context.SENSOR_SERVICE);
    Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    if (sensor == null)
    {
      Log.w("SensorHelper", "WARNING: There is no ROTATION_VECTOR sensor, requesting GEOMAGNETIC_ROTATION_VECTOR");
      sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
      if (sensor == null)
        Log.w("SensorHelper", "WARNING: There is no GEOMAGNETIC_ROTATION_VECTOR sensor, device orientation can not be calculated");
    }
    // Can be null in rare cases on devices without magnetic sensors.
    mRotationVectorSensor = sensor;
  }

  void start()
  {
    if (mRotationVectorSensor != null)
      mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
  }

  void stop()
  {
    if (mRotationVectorSensor != null)
      mSensorManager.unregisterListener(this);
  }
}
