package com.firefly.app.engine

class InertialOdometer3D(private var vel: Double,
                         private var pos: Double
) {
    private var acc: Double? = null
    private var timestamp: Long? = null

    fun addDataPoint(newAcc: Double, newTimeStamp: Long): Pair<Double, Double> {
        if (timestamp == null) {
            timestamp = newTimeStamp
        }
        val delta = (newTimeStamp - timestamp!!) * 1e-9
        val preVel = vel
        if (acc == null) {
            acc = newAcc
        }
        vel += delta * ((newAcc + acc!!) / 2)
        pos += delta * ((preVel + vel) / 2)
        timestamp = newTimeStamp
        acc = newAcc
        return Pair(pos, vel)
    }
}
