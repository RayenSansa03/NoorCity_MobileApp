package tn.esprit.sansa.data.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import tn.esprit.sansa.ui.screens.models.Sensor
import tn.esprit.sansa.ui.screens.models.SensorStatus
import tn.esprit.sansa.ui.screens.models.SensorType

class FirebaseSensorsRepository {
    private val database = FirebaseDatabase.getInstance("https://smartcity-35df0-default-rtdb.firebaseio.com/").getReference("sensors")

    fun getSensors(): Flow<List<Sensor>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensors = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val typeStr = child.child("type").getValue(String::class.java) ?: "LIGHT"
                    val value = child.child("value").getValue(String::class.java) ?: "0"
                    val statusStr = child.child("status").getValue(String::class.java) ?: "ACTIVE"
                    val battery = child.child("battery").getValue(Int::class.java) ?: 100
                    val streetlightName = child.child("streetlightName").getValue(String::class.java) ?: "Sansa Light"
                    
                    // Nouveaux champs AI
                    val humidity = child.child("humidity").getValue(Double::class.java) ?: 0.0
                    val heatIndex = child.child("heatIndex").getValue(Double::class.java) ?: 0.0
                    val riskLevel = child.child("riskLevel").getValue(Int::class.java) ?: 0

                    Sensor(
                        id = id,
                        type = SensorType.valueOf(typeStr),
                        streetlightId = "L001",
                        streetlightName = streetlightName,
                        currentValue = value,
                        status = try { SensorStatus.valueOf(statusStr) } catch(e: Exception) { SensorStatus.ACTIVE },
                        lastUpdate = "En direct",
                        batteryLevel = battery,
                        humidity = humidity,
                        heatIndex = heatIndex,
                        riskLevel = riskLevel
                    )
                }
                trySend(sensors)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        database.addValueEventListener(listener)
        awaitClose { database.removeEventListener(listener) }
    }

    fun addSensor(sensor: Sensor, onComplete: (Boolean) -> Unit = {}) {
        val sensorMap = mapOf(
            "type" to sensor.type.name,
            "value" to sensor.currentValue,
            "status" to sensor.status.name,
            "battery" to sensor.batteryLevel,
            "streetlightName" to sensor.streetlightName,
            "humidity" to sensor.humidity,
            "heatIndex" to sensor.heatIndex,
            "riskLevel" to sensor.riskLevel
        )
        database.child(sensor.id).setValue(sensorMap)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun deleteSensor(id: String) {
        database.child(id).removeValue()
    }
}
