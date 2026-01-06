/*
 * ESP32 - Temperature & Humidity Node (Sansa Project)
 * Transmet les données d'un capteur DHT11/DHT22 vers Firebase en temps réel.
 * 
 * Bibliothèques requises (Arduino IDE) :
 * 1. DHT sensor library by Adafruit
 * 2. Firebase ESP32 Client by Mobizt
 */

#include <WiFi.h>
#include <FirebaseESP32.h>
#include "DHT.h"

// --- CONFIGURATION SENSOR ---
#define DHTPIN 14          // Broche DATA du DHT (GPIO 14)
#define DHTTYPE DHT11     // Changez en DHT22 si vous utilisez un DHT22
DHT dht(DHTPIN, DHTTYPE);

// --- CONFIGURATION WIFI ---
const char* WIFI_SSID = "TOPNET_1A98";
const char* WIFI_PASSWORD = "prt9tzb739";

// --- CONFIGURATION FIREBASE ---
const char* FIREBASE_HOST = "smartcity-35df0-default-rtdb.firebaseio.com";
const char* FIREBASE_AUTH = "0cBvCPFZ6OYavJ9xJccNg59sVMJ73IQvNJzZWJFc"; 

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

String SENSOR_ID; // Sera généré automatiquement à partir de l'adresse MAC
unsigned long sendDataPrevMillis = 0;

void setup() {
  Serial.begin(115200);

  // Utilisation du pull-up interne
  pinMode(DHTPIN, INPUT_PULLUP); 
  dht.begin();

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println("\nConnected!");

  // Génération de l'ID Unique à partir de la MAC (ex: TEMP_A1B2C3)
  String mac = WiFi.macAddress();
  mac.replace(":", "");
  SENSOR_ID = "TEMP_" + mac.substring(6); // Utilise les 6 derniers caractères pour plus de clarté
  
  Serial.println("------------------------------------");
  Serial.print("VOTRE ID CAPTEUR UNIQUE : ");
  Serial.println(SENSOR_ID);
  Serial.println("------------------------------------");

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

// --- TINYML MODEL (Exported from Python) ---
// 0=Safe, 1=Caution, 2=Danger, 3=Extreme
int predictHeatRisk(float t, float h) {
    if (t < 26.7) return 0; // SAFE

    if (t < 30.0) {
        if (h < 60) return 0; // SAFE
        else return 1;        // CAUTION
    }

    if (t < 35.0) {
        if (h < 35) return 0; // SAFE
        if (h < 65) return 1; // CAUTION
        return 2;             // DANGER
    }

    if (t < 40.0) {
        if (h < 25) return 1; // CAUTION
        if (h < 55) return 2; // DANGER
        return 3;             // EXTREME
    }

    // t >= 40.0
    if (h < 20) return 2;     // DANGER
    return 3;                 // EXTREME
}

// Formule Rothfusz (Heat Index Value)
float calculateHeatIndexValue(float T, float RH) {
    if (T < 26.7) return T;
    float Tf = (T * 1.8) + 32;
    float HI = -42.379 + 2.04901523*Tf + 10.14333127*RH - .22475541*Tf*RH \
         - .00683783*Tf*Tf - .05481717*RH*RH + .00122874*Tf*Tf*RH \
         + .00085282*Tf*RH*RH - .00000199*Tf*Tf*RH*RH;
    return (HI - 32) / 1.8;
}

// Variables pour la simulation (Demo Mode)
float simTemp = 28.0;
float simHum = 50.0;
bool simDirectionUp = true;

void loop() {
  if (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0) { // Update every 5s for demo
    sendDataPrevMillis = millis();

    // 1. Lecture Réelle (Si capteur connecté)
    float t = dht.readTemperature();
    float h = dht.readHumidity();

    // 2. SIMULATION vs RÉEL
    // Si la lecture échoue (isnan), on passe en simulation pour éviter de planter
    // SINON, on utilise les vraies valeurs du capteur DHT11 (Branché sur GPIO 14)
    if (isnan(t) || isnan(h)) { 
       Serial.println("⚠️ Erreur capteur ! Passage en mode Simulation...");
       
       // Variation lente pour simuler une journée qui chauffe
       if (simDirectionUp) {
          simTemp += 0.5;
          simHum += 2.0;
          if (simTemp > 38.0) simDirectionUp = false;
       } else {
          simTemp -= 0.5;
          simHum -= 2.0;
          if (simTemp < 25.0) simDirectionUp = true;
       }
       t = simTemp;
       h = simHum;
    }

    // 3. TinyML Inference
    int riskLevel = predictHeatRisk(t, h);
    float heatIndex = calculateHeatIndexValue(t, h);

    Serial.printf("ID: %s | Temp: %.1f°C | Hum: %.1f%% | HeatIndex: %.1f°C | Risk: %d\n", 
                  SENSOR_ID.c_str(), t, h, heatIndex, riskLevel);

    String path = "/sensors/" + SENSOR_ID;
    
    FirebaseJson updateData;
    updateData.add("temperature", t); // Send raw float
    updateData.add("humidity", h);
    updateData.add("heatIndex", heatIndex); // AI Calculated
    updateData.add("riskLevel", riskLevel); // AI Predicted class
    updateData.add("lastUpdate", millis());
    updateData.add("status", "ACTIVE");
    
    // Legacy value for simple display
    updateData.add("value", String(t, 1));

    if (Firebase.updateNode(fbdo, path, updateData)) {
      Serial.println("Envoi réussi vers Firebase.");
    } else {
      Serial.println("Erreur Firebase: " + fbdo.errorReason());
    }
  }
}
