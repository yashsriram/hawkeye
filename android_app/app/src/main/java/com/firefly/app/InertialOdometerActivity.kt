package com.firefly.app

import android.content.Context
import android.hardware.Sensor
import android.os.Bundle
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import androidx.appcompat.app.AppCompatActivity
import com.firefly.app.engine.InertialOdometer3D
import kotlinx.android.synthetic.main.activity_inertial_odometer.*
import java.text.DecimalFormat


class InertialOdometerActivity : AppCompatActivity(), SensorEventListener {
    private val sensorType = Sensor.TYPE_LINEAR_ACCELERATION
    private val samplingFrequency = SensorManager.SENSOR_DELAY_FASTEST
    private val df = DecimalFormat("#.####")

    private var sampleSizeLimit = 1000
    private var sensorManager: SensorManager? = null
    private var iox: InertialOdometer3D? = null
    private var ioy: InertialOdometer3D? = null
    private var ioz: InertialOdometer3D? = null
    private var epoch: Long? = null

    private var noSamples = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inertial_odometer)
        // inertial odometer
        iox = InertialOdometer3D(0.0, 0.0)
        ioy = InertialOdometer3D(0.0, 0.0)
        ioz = InertialOdometer3D(0.0, 0.0)
        epoch = System.nanoTime()
        // sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(sensorType),
            samplingFrequency
        )
        sampleSizeLimitView.setText(sampleSizeLimit.toString())
        // restartBtn
        restartBtn.setOnClickListener({
            try {
                sampleSizeLimit = sampleSizeLimitView.text.toString().toInt()
            } catch (e: Exception) {
                sampleSizeLimit = 1000
                sampleSizeLimitView.setText(sampleSizeLimit.toString())
            }
            iox = InertialOdometer3D(0.0, 0.0)
            ioy = InertialOdometer3D(0.0, 0.0)
            ioz = InertialOdometer3D(0.0, 0.0)
            epoch = System.nanoTime()
            noSamples = 0
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == sensorType) {
            collectSample(
                event.values[0].toDouble(),
                event.values[1].toDouble(),
                event.values[2].toDouble(),
                System.nanoTime()
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    private fun collectSample(ax: Double, ay: Double, az: Double, t: Long) {
        if (noSamples < sampleSizeLimit) {
            val (sx, vx) = iox!!.addDataPoint(ax, t)
            axView.post({ axView.text = df.format(ax).toString() })
            vxView.post({ vxView.text = df.format(vx).toString() })
            sxView.post({ sxView.text = df.format(sx).toString() })

            val (sy, vy) = ioy!!.addDataPoint(ay, t)
            ayView.post({ ayView.text = df.format(ay).toString() })
            vyView.post({ vyView.text = df.format(vy).toString() })
            syView.post({ syView.text = df.format(sy).toString() })

            val (sz, vz) = ioz!!.addDataPoint(az, t)
            azView.post({ azView.text = df.format(az).toString() })
            vzView.post({ vzView.text = df.format(vz).toString() })
            szView.post({ szView.text = df.format(sz).toString() })

            timeElapsedView.post({
                timeElapsedView.text = ((t - epoch!!) * 1e-6).toInt().toString()
            })
            noSamples++
        }
    }
}