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
  @Nullable
  private final SensorManager mSensorManager;
  @Nullable
  private Sensor mRotation;
  @NonNull
  private final MwmApplication mMwmApplication;

  @Override
  public void onSensorChanged(SensorEvent event)
  {
    if (!mMwmApplication.arePlatformAndCoreInitialized())
      return;

    notifyInternal(event);
  }

  private final static int kRotationVectorSensorType = Sensor.TYPE_ROTATION_VECTOR;
  private final float[] mRotationMatrix = new float[9];
  private final float[] mRotationValues = new float[3];

  private void notifyInternal(@NonNull SensorEvent event)
  {
    if (event.sensor.getType() == kRotationVectorSensorType)
    {
      SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
      SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mRotationMatrix);

      SensorManager.getOrientation(mRotationMatrix, mRotationValues);

      // mRotationValues indexes: 0 - yaw, 2 - roll, 1 - pitch.
      LocationHelper.INSTANCE.notifyCompassUpdated(mRotationValues[0]);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {
    Log.i("onAccuracyChanged", "Sensor " + sensor.getStringType() + " has changed accuracy to " + accuracy);
    if (sensor.getType() == kRotationVectorSensorType)
    {
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
  }

  SensorHelper(@NonNull Context context)
  {
    mMwmApplication = MwmApplication.from(context);
    mSensorManager = (SensorManager) mMwmApplication.getSystemService(Context.SENSOR_SERVICE);
    if (mSensorManager != null)
      mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
  }

  void start()
  {
    if (mRotation != null && mSensorManager != null)
      mSensorManager.registerListener(this, mRotation, SensorManager.SENSOR_DELAY_UI);
  }

  void stop()
  {
    if (mSensorManager != null)
      mSensorManager.unregisterListener(this);
  }
}
