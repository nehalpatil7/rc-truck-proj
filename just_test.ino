#include <Servo.h>
#include <SoftwareSerial.h>
SoftwareSerial bluetooth(0, 1);  //RX,TX
String readstring;
Servo head;
int directionSwitch = 1;         //1-forward, 2=reverse
int sped = 0;
int handbrake_stat = 1;
int angle = 95;
int parking = 0;

#define SERVO_PIN 7
#define front 90k
const int bluetoothConnectedStatus = 2;
int motors_forward = 5;
int motors_back = 6;
int buzzer = 3;
int all_lights_on = 8;
int headlights = 12;

void setup() {
  //this setup code will execute once everytime when the arduino will power up
  Serial.begin(9600);
  pinMode(motors_forward, OUTPUT);
  pinMode(motors_back, OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(all_lights_on, OUTPUT);
  pinMode(headlights, OUTPUT);
  head.attach(11);
  turn(angle);
  bluetooth.begin(9600);
  delay(100);
}

void loop() {

  while (bluetooth.available()) {                    //this while condition will execute indefinitely
    char data = bluetooth.read();
    if (isdigit(data)) {                    //if readData is digit
      readstring += data;
      sped = readstring.toInt();
    }
    else if (data == 'a') {                 //for SPEED ; if read data is alphabet
      if (handbrake_stat == 0 && directionSwitch == 1) {
        forwards(sped);
      } else if (handbrake_stat == 0 && directionSwitch == 2) {
        reverse(sped);
      }
      readstring = "";
    }
    else if (data == 't') {           //steer - turn vehicle
      angle = sped;
      turn(angle);
      readstring = "";
    }
    else if (data == 'c') {           //speed slow on slider touch removal
      if(handbrake_stat==0){
        slow();
      }
      readstring = "";
    }
    else if (data == 'f') {         //switch to forward
      directionSwitch = 1;
    }
    else if (data == 'b') {         //switch to reverse
      directionSwitch = 2;
    }
    else if (data == 'x') {         //apply handbrake
      handbrake_stat = 1;
      handbrake();
    }
    else if (data == 'y') {         //remove handbrake
      handbrake_stat = 0;
    }
    else if (data == 'l') {         //turn headlights ON
      headlightOn();
    }
    else if (data == 'm') {         //turn headlights OFF
      headlightOff();
    }
    else if (data == 'h') {         //turn horn ON for 1 second
      blowHorn();
    }
    else if (data == 'p') {         //turn parking mode ON
      parking = 1;
      parkinglightsOn();
    }
    else if (data == 'q') {         //turn parking mode OFF
      parking = 0;
      parkinglightsOff();
    }

  } //while END
} //loop END


//move forward
void forwards(int speeed) {

  if(parking==1 && speeed>=40){
    speeed=40;
  }
    analogWrite(motors_forward, speeed);

}

//move reverse
void reverse(int speeed) {

  if(parking==1 && speeed>=40){
    speeed=40;
  }
    analogWrite(motors_back, speeed);

}

//speed slow on slider touch removal
void slow() {
  if (directionSwitch == 1) {
    analogWrite(motors_forward, 70);
  }
  else if (directionSwitch == 2) {
    analogWrite(motors_back, 70);
  }
  delay(200);
  digitalWrite(motors_forward, LOW);
  digitalWrite(motors_back, LOW);
}

//turn
void turn(int angle) {
  head.write(angle);
}

//handbrake - urgent brake
void handbrake() {
  digitalWrite(motors_forward, LOW);
  digitalWrite(motors_back, LOW);
  handbrake_stat = 1;
}

void blowHorn() {
  //The limitations of the tone() function include:
  //Not being able to use PWM on pins 3 and 11 while you use tone()
  //You can’t go lower than 31 hertz.
  //When using tone() on different pins,
  //you have to turn off the tone on the current pin with noTone(),
  //before using tone() on a different pin.
  //use 1000-frequency for the reverse gear sound

  tone( buzzer, 40, 2000);
}

void headlightOn() {
  //make headlights pin HIGH
  digitalWrite(headlights, HIGH);
}

void headlightOff() {
  //make headlights pin LOW
  digitalWrite(headlights, LOW);
}

void parkinglightsOn() {
  //limit speed to 50 here & make both indicator pins HIGH
  digitalWrite(all_lights_on, HIGH);
}

void parkinglightsOff() {
  //remove speed limit & make indicator pins LOW
  digitalWrite(all_lights_on, LOW);
}
