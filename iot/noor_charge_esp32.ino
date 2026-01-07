#include <WiFi.h>
#include <FirebaseESP32.h>
#include <ESP32Servo.h>
#include <SevSeg.h> 

// --- Configuration ---
#define WIFI_SSID "TOPNET_1A98"
#define WIFI_PASSWORD "prt9tzb739"
#define FIREBASE_HOST "smartcity-35df0-default-rtdb.firebaseio.com" 
#define FIREBASE_AUTH "0cBvCPFZ6OYavJ9xJccNg59sVMJ73IQvNJzZWJFc"
#define STREETLIGHT_ID "LAMP-001" 

// --- Pins ---
const int PIN_SERVO = 13; 
const int PIN_LED_BLUE = 2; // LED Bleue intégrée à la carte ESP32
const int PIN_LED_RED = 25;  // LED Rouge externe

// --- Objets ---
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
Servo myServo;
SevSeg sevseg; 

// --- Variables ---
bool isCharging = false;
unsigned long lastFirebaseCheck = 0;

void setup() {
  Serial.begin(115200);

  // 1. Setup Matériel
  myServo.attach(PIN_SERVO);
  pinMode(PIN_LED_BLUE, OUTPUT);
  pinMode(PIN_LED_RED, OUTPUT);
  
  // Setup Afficheur 3461BS (4 digits, 4x7 segments)
  byte numDigits = 4;
  
  // Mapping ESP32 -> Segments (A, B, C, D, E, F, G, DP)
  // ⚡ IMPORTANT: J'ai déplacé le segment B du GPIO 2 au GPIO 14 pour libérer la LED Bleue
  byte segmentPins[] = {15, 14, 0, 4, 16, 17, 5, 18}; 
  
  // Mapping ESP32 -> Digits (D1, D2, D3, D4)
  byte digitPins[] = {19, 21, 22, 23}; 
  
  bool resistorsOnSegments = true; 
  byte hardwareConfig = COMMON_ANODE; 
  bool updateWithDelays = false;
  
  sevseg.begin(hardwareConfig, numDigits, digitPins, segmentPins, resistorsOnSegments, updateWithDelays);
  sevseg.setBrightness(90);

  // État initial (Libre)
  updateHardware(false); 

  // 2. Connexion WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED && millis() < 10000) {
    delay(500);
    Serial.print(".");
  }

  // 3. Connexion Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  sevseg.refreshDisplay(); 

  if (millis() - lastFirebaseCheck > 500) {
    checkFirebase();
    lastFirebaseCheck = millis();
  }
}

void checkFirebase() {
  if (WiFi.status() != WL_CONNECTED) return;

  FirebaseData statusData;
  if (Firebase.getBool(statusData, "/streetlights/" STREETLIGHT_ID "/isChargingActive")) {
    bool active = statusData.boolData();
    if (active != isCharging) {
      isCharging = active;
      updateHardware(isCharging);
    }
  }

  if (isCharging) {
    FirebaseData timeData;
    if (Firebase.getInt(timeData, "/streetlights/" STREETLIGHT_ID "/timeRemaining")) {
       int sec = timeData.intData();
       int mins = sec / 60;
       int s = sec % 60;
       sevseg.setNumber(mins * 100 + s, 2); 
    }
  } else {
    sevseg.setChars("LIBRE"); 
  }
}

/**
 * Met à jour les actionneurs selon le paiement (isChargingActive)
 * - Si payé (active=true) : LED Rouge OFF, LED Bleue ON, Servo 90°
 * - Si libre (active=false) : LED Rouge ON, LED Bleue OFF, Servo 0°
 */
void updateHardware(bool active) {
  if (active) {
    // 💳 Paiement effectué / Recharge active
    digitalWrite(PIN_LED_RED, LOW);    // Éteint la LED Rouge
    digitalWrite(PIN_LED_BLUE, HIGH);  // Allume la LED Bleue intégrée
    myServo.write(90);                 // Tourne le Servo (Ouverture)
    Serial.println("PAIEMENT REÇU : Activation de la recharge !");
  } else {
    // 🔋 Libre / Recharge terminée
    digitalWrite(PIN_LED_BLUE, LOW);   // Éteint la LED Bleue
    digitalWrite(PIN_LED_RED, HIGH);   // Allume la LED Rouge
    myServo.write(0);                  // Remet le Servo (Fermeture)
    Serial.println("RECHARGE TERMINEE : Session fermée.");
  }
}
