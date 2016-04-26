/*
 * Links:
 * http://www.arduino.cc/en/Hacking/PinMapping2560
 * http://www.arduino.cc/en/Reference/PortManipulation
 * http://www.arduino.cc/en/Tutorial/Button
 *
 * Port PL:
 *     PL0 -> Digital pin 49 -> Button 0
 *     PL1 -> Digital pin 48 -> Button 1
 *     ...
 *     PL7 -> Digital pin 42 -> Button 7
 */

byte valSent = 0;
byte valRead = 0;
long timeRead;
const long timedelay = 50;
const byte mask = B10000000;
long ledOnTime = 0;
bool ledOn = false;
long ledTimeout = 300 * 1000L;
int ledPin = 3;

void setup() {
  // configure LED output pin
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW);
  Serial.begin(9600);
  // Set PL0..PL6 (pins 49 to 43) as inputs, without
  // changing PL7 (pin 42).
  DDRL = DDRL & mask;
  valSent = PINL;
  valRead = valSent;
  timeRead = millis();
}

void loop() {
  long timex = millis();
  byte v = PINL & (~mask);
  
  if(valRead != v) {
    valRead = v;
    timeRead = timex;
  }

  if(valRead != valSent && (timex - timeRead > timedelay)) {
    valSent = valRead;
    Serial.write(valSent);
    // Serial.println(String(valSent, BIN));
    
    if(valSent > 0) {
      if(!ledOn) {
        // Serial.println("switching on");
        ledOn = true;
        digitalWrite(ledPin, HIGH);
      }
      ledOnTime = timex;
    }
  }
  
  // Serial.println(String(timex - ledOnTime));
  // Serial.println(String(ledTimeout));
  if(ledOn && ((timex - ledOnTime) > ledTimeout)) {
    // Serial.println("switching off");
    ledOn = false;
    digitalWrite(ledPin, LOW);
  }
  // digitalWrite(ledPin, HIGH);
}
