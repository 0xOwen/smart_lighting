
#include<SoftwareSerial.h>


const byte rxpin = 3;
const byte txpin = 4;

SoftwareSerial bluetooth(rxpin, txpin);   // initialize RX,TX


const int LED1 = 9;
const int LED2 = 10;
const int LED3 = 11;
char status = '0';

void setup() 
{
  Serial.begin(9600);
  bluetooth.begin(9600);
  pinMode(LED1,OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(LED3, OUTPUT);
  digitalWrite(9, HIGH);
  delay(5000);
  digitalWrite(9, LOW);
  
  Serial.println("Ready for bluetooth Connection.\n The bluetooth password is either 0000 or 1234");

}

void loop() 
{
    if(bluetooth.available() > 0)
    {
      status = bluetooth.read();
    }
    
    if (status == '1')
    {
      digitalWrite(9, HIGH);
    }
    else if (status == '0')
    {
      digitalWrite(9, LOW);
    }   
}
