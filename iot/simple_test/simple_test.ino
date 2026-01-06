#include <ESP32Servo.h>

const int PIN_SERVO = 13;
const int PIN_LED_GREEN = 12;
const int PIN_LED_RED = 25;
const int PIN_BUILTIN = 2; // LED Bleue sur la carte ESP32

Servo myServo;

void setup() {
  Serial.begin(115200);
  Serial.println("--- START TEST ---");

  myServo.attach(PIN_SERVO);
  pinMode(PIN_LED_GREEN, OUTPUT);
  pinMode(PIN_LED_RED, OUTPUT);
  pinMode(PIN_BUILTIN, OUTPUT);
}

void loop() {
  Serial.println("Testing RED ON + BUILTIN ON");
  digitalWrite(PIN_LED_RED, HIGH);
  digitalWrite(PIN_LED_GREEN, LOW);
  digitalWrite(PIN_BUILTIN, HIGH); // Allume la LED de la carte
  myServo.write(0);
  delay(1000);

  Serial.println("Testing GREEN ON + SERVO 90 + BUILTIN OFF");
  digitalWrite(PIN_LED_RED, LOW);
  digitalWrite(PIN_LED_GREEN, HIGH);
  digitalWrite(PIN_BUILTIN, LOW); // Eteint la LED de la carte
  myServo.write(90);
  delay(1000);
}
