#include "BluetoothSerial.h"
#include <WiFi.h>
#include "FS.h"
#include <LittleFS.h>
#include <Arduino.h>
#include <FirebaseClient.h>
#include <WiFiClientSecure.h>
#include <ArduinoJson.h>
#include "DHT.h"
#include <ESP32Servo.h>
#include "time.h"

#define DHTPIN 4
#define MQ2PIN 34
#define KY026PIN 35
#define KY026PIN_2 32
#define RESETPIN 22
#define BUZZERPIN 18
#define SERVO1PIN 17
#ifndef BUILTIN_LED
#define BUILTIN_LED 2
#endif

#define FORMAT_LITTLEFS_IF_FAILED true

#define API_KEY "AIzaSyD6hG7wbwkZctD3eLo4orYjQ7QlDmBtvHs"
#define DATABASE_URL "https://esp32-kebakaran-default-rtdb.asia-southeast1.firebasedatabase.app"

#define FIREBASE_PROJECT_ID "esp32-kebakaran"
#define FIREBASE_CLIENT_EMAIL "firebase-adminsdk-w4wgj@esp32-kebakaran.iam.gserviceaccount.com"
const char PRIVATE_KEY[] PROGMEM = "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDMFYipV5xXN0T0\n9Am6IvpO5NeEgmoMyhC7Gt009Sa3kqACAqeG2gJi4oM8u8stjzdiRtVnmuE0OTiK\no5zQZZ5Ll+qQi1cYsNl/bSgDRcfX6y4Sm6v5yFWTxIYbUNJW2Y83jaza3m24VpnN\nn+SZAXReVQmlTUKaIe+YBuft1DaZZsI0+ZNpA87+8ZRyAU8Nmm3tjsZrMzhNn6dJ\nZglSpUvpRToLLLJNi3gmh0yz1sdYEOHvE1/Glbdma7oLgGpa8d5pVkAoiLqgb2yy\nJUiZ+W817Kh3Mkkro1ufDazkgsXMy3jWxdbpIV89Y7dRo5Vm0+SFxz1XIhEJkHrv\n34Ng7eihAgMBAAECggEAAmC8QOZ2bRKFySmp9PNA+15YBhrTskzaBdp54Irispmq\nWs7XgnWHBZciDexNbiOqP4jkjHQV7HSmOTQcbAkv32DO9adjkmdGtK3Ig5E/YAxF\nDMGnlVESPjCFNEt5LlRGnB0Yehkq9ugVAeJVC+6KaCorFjP6P+JMil1T1axdUbHT\n4Tv/8Fl9nfrCBGnAtyg4B5yYHtlS+3jkpEcQg8dDaGXys3DpUjwq8yZlwlONgwrL\nLKagesF0mYTJiktR+BRq800X+cNV0WyDyVcsURz6b3fnxss1xrda8k76kJJITI58\nqDiiZaefD8hc6YIVTeFzZRAkMrJEd2uROf+e3a/ZYQKBgQDyDwaZrvkyX2n1/JWY\nW3grCWnMcl3L+vIdYvHMV18Z0AYPbjm22xch1YNk5PrrnIiVwy9OlAOoOpFubfso\njn/5o9jofkk6BLvBLdn52M/03NP+ERnDkd6P2RvLQo7pPI9oQ6hQ0cmduBfNWYKI\nZKW9zrmUe2zm8DDA5OzjUCmKBQKBgQDX1pnw5Jva48u+YnSWm9PzSqg3thpob2ra\nGlDWg3Y1bjcFIqTynUUjc9URRV39uqAYU4R53qUMsvboBH2GZECZ5yJlUwt6L17H\nugPyQzVuIQ/MwzveIeekMR356/X7y8fLRmDnWh6Fxu5YpXT+iaNUrHsQ6I855Mt2\nMuTs3qQ67QKBgQDk68j2N0B9nzb66LIhsP/o07I3JEII039+w0CiXE8Yfl/83+W9\nRV6PJPpqxRN5GEUkPb/TtK/wvdMkeOb66Cmn0okk96oRMMDQZaisXZvum5nxwgJ3\nrtCpgk6C4KN/eUK2/KWGKaFfg+ce0fRdQbJt28Au6LuJfMbe5qISKJHgcQKBgQCF\n32SiJpmI3eu8jYK0+7gt2euWiou8pQbIerQTKX0DTPYxaDiZBvUgNTM9wCG5Q60T\nCZJMqM0EoGhGpqpY1gu/qb4KzMR8p68JAJZ8CDW2tij/n0Sd1bfAfnqSTknHQLEO\novtt49a0sRuavh1f120IEudOKIDVrtSH3q9hQALrTQKBgF54ePIEp7r4jUU/9uK1\nDKmYbzoj4Rhy9ftNPcYAXzNnfgU5zAWinwW2qBLWwRXuI0iZSlNnGqo4k6J2IzUt\n38D2fQ2JFLunNpBPCpMDP+rLNt4Fq+2HZwROiMePBdKywzRvlxLaijQUpsmqTsxC\nhJdbWQYXg/2oAOvR+DdQy9zZ\n-----END PRIVATE KEY-----\n";

void asyncCB(AsyncResult &aResult);
void printResult(AsyncResult &aResult);

void getMsg(Messages::Message &msg, const String token, const String title, const String body);
void timeStatusCB(uint32_t &ts);

DefaultNetwork network;
FirebaseApp app;
WiFiClientSecure ssl_client, ssl_client2;
using AsyncClient = AsyncClientClass;
AsyncClient aClient(ssl_client, getNetwork(network)), aClient2(ssl_client2, getNetwork(network));
RealtimeDatabase Database;
AsyncResult aResult1, aServoTask;
ServiceAuth sa_auth(timeStatusCB, FIREBASE_CLIENT_EMAIL, FIREBASE_PROJECT_ID, PRIVATE_KEY, 3000);
Messaging messaging;

String device_name = "";
String bt_recv_buffer = "";
String bt_send_buffer = "";

BluetoothSerial SerialBT;

const char* timeServer = "pool.ntp.org";
const long  gmtOffset_sec = 25200;
const int   daylightOffset_sec = 0;
unsigned long epochTime;

const int pollingTimes = 15;

const int RL = 5;
float R0 = 4.03;
int smokeValue = 0;
float smokePPM = 0.0;
int smokeReading[pollingTimes];

int flameValue = 0;
int flameValue2 = 0;
int flameReading[pollingTimes];

const int smokePPMThreshold = 150;
const int flameThreshold = 3500;
const float tempThreshold = 39.0;

float lastTemperature = 0.0;
float lastHumidity = 0.0;
float humidity = 0.0;
float temperature = 0.0;
unsigned long prevMillisDHT = 0;

int burnInTime = 60000;

int freshAirThreshold = 1000;

bool warningKebakaran = false;
bool sendNotificationKebakaran = false;

unsigned long KebakaranMillis = 0;
int KebakaranLastingTime = 10000;

unsigned long KebakaranEndMillis = 0;
unsigned long KebakaranEndLastingTime = 15000;

bool openedServo = false;

Servo myservo;

const int pos_tutup = 100;
const int pos_buka = 30;

bool servoState = false;
bool getServoState = false;

bool setupFailed = false;

char* ssid;
char* password;
char* owner_uid;

StaticJsonDocument<512> jsonDocRecv;
StaticJsonDocument<512> jsonDocSend;

unsigned long lastUpdateMillis = 0;
const unsigned long updateMillis = 6000;

unsigned long resetButtonPressTime = 0;
bool resetButtonPressed = false;
int resetButtonTime = 3000;

unsigned long prevMillisBuzzer = 0;
unsigned long prevMillis = 0;

time_t now;

uint32_t chipId = 0;
char strChipId[20];

DHT dht;

void setup() {
    Serial.begin(115200);

    for(int i=0; i<17; i=i+8) {
	    chipId |= ((ESP.getEfuseMac() >> (40 - i)) & 0xff) << i;
	  }

    sprintf(strChipId, "%08X", chipId);
    device_name = "ESP32-" + String(strChipId);

    Serial.println(device_name);

    if (LittleFSSetup()) {
        readFile(LittleFS, "/ssid", ssid);
        readFile(LittleFS, "/password", password);
        readFile(LittleFS, "/owner_uid", owner_uid);
    }else{
        Serial.println("Failed to mount LittleFS");
        ESP.restart();
    }

    pinMode(BUILTIN_LED, OUTPUT);
    pinMode(MQ2PIN, INPUT);
    pinMode(KY026PIN, INPUT);
    pinMode(KY026PIN_2, INPUT);
    pinMode(BUZZERPIN, OUTPUT);
    pinMode(RESETPIN, INPUT_PULLUP);

    dht.setup(DHTPIN);

    myservo.attach(SERVO1PIN, 500, 2500);

    Serial.println("heat up mq2 sensor...");

    delay(10000);

    smokeValue = readSmokeSensor();
    float ratio = calculateRatio(smokeValue);
    float ppm = getSmokePPM(ratio);

    Serial.print("Smoke Value: ");
    Serial.println(smokeValue);
    Serial.print("Smoke PPM: ");
    Serial.println(ppm);

    if (ppm < smokePPMThreshold){
        burnInTime = 0;
        Serial.println("Burn in time 0");
    }else{
        Serial.println("Burn in time");
    }

    myservo.write(pos_tutup);
    delay(500);
    myservo.write(pos_tutup-5);

    WiFi.mode(WIFI_STA);
    WiFi.setHostname(device_name.c_str());

    if (ssid != NULL && password != NULL) {
        WiFi.begin(ssid, password);

        Serial.printf("Connecting to %s\n", ssid);
        for (int i = 0; i < 30; i++)
        {
            if (WiFi.status() == WL_CONNECTED){
                break;
            }else{
                Serial.print(".");
                delay(1000);
            }
        }

        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("Connection Failed!");

            SerialBT.begin(device_name);
            Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());

            setupFailed = true;
            return;
        }
 
        Serial.println("Connected to the WiFi network.");
        IPAddress localIP = WiFi.localIP();
        Serial.print("IP Address: ");
        Serial.println(localIP);
    } else {
        WiFi.disconnect();
        SerialBT.begin(device_name);
        Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());
        setupFailed = true;
        return;
    }

    configTime(gmtOffset_sec, daylightOffset_sec, timeServer);

    Firebase.printf("Firebase Client v%s\n", FIREBASE_CLIENT_VERSION);
    Serial.println("Initializing app...");
    ssl_client.setInsecure();
    ssl_client2.setInsecure();

    initializeApp(aClient, app, getAuth(sa_auth), asyncCB, "authTask");
    
    app.getApp <Messaging> (messaging);
    app.getApp <RealtimeDatabase> (Database);
    Database.url(DATABASE_URL);

    struct tm timeinfo;
    for (int i = 0; i < 15; i++)
    {
        if (!getLocalTime(&timeinfo)){
            Serial.print(".");
            delay(1000);
        }else{
            break;
        }
    }

    printLocalTime();

    Database.set<bool>(aClient, "/uid/" + String(owner_uid) + "/devices/" + device_name + "/servo", false, asyncCB, "servoTask");
    Database.get(aClient, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/servo", aServoTask, true /* this option is for Stream connection */);

    digitalWrite(BUZZERPIN, HIGH);
    delay(10);
    digitalWrite(BUZZERPIN, LOW);
}

bool LittleFSSetup() {
    if (!LittleFS.begin(FORMAT_LITTLEFS_IF_FAILED)) {
        Serial.println("LittleFS Mount Failed");
        return false;
    } else {
        Serial.println("Little FS Mounted Successfully");
        return true;
    }
}

void writeFile(fs::FS & fs,
        const char * path,
        const char * message) {
    Serial.printf("Writing file: %s\r\n", path);
    Serial.printf("Message: %s\r\n", message);

    File file = fs.open(path, FILE_WRITE);
    if (!file) {
        Serial.println("- failed to open file for writing");
        return;
    }
    if (file.print(message)) {
        Serial.println("- file written");
    } else {
        Serial.println("- write failed");
    }
    file.close();
}

void readFile(fs::FS & fs,
    const char * path, char * & buffer) {
    Serial.printf("Reading file: %s\r\n", path);

    File file = fs.open(path);
    if (!file || file.isDirectory()) {
        Serial.println("- failed to open file for reading");
        return;
    }

    Serial.print("- read from file: ");
    String content = file.readString();
    Serial.println(content);

    buffer = (char *)malloc(content.length() + 1);
    if (buffer) {
        strcpy(buffer, content.c_str());
    } else {
        Serial.println("- failed to allocate memory for buffer");
    }

    file.close();
}

int getMedian(int *array, int size) {
  int tempArray[size];
  memcpy(tempArray, array, size * sizeof(int));
  for (int i = 0; i < size - 1; i++) {
    for (int j = i + 1; j < size; j++) {
      if (tempArray[i] > tempArray[j]) {
        int temp = tempArray[i];
        tempArray[i] = tempArray[j];
        tempArray[j] = temp;
      }
    }
  }
  return tempArray[size / 2];
}

int readSmokeSensor(){
    for (int i = 0; i < pollingTimes+1; i++) {
        smokeReading[i] = analogRead(MQ2PIN);
    }

    return getMedian(smokeReading, pollingTimes);
}

float calculateRatio(int adcValue) {
  float voltage = adcValue * (3.3 / 4095.0);
  float Rs = 3.3 * (RL/voltage) - RL;
  
  float ratio = Rs / R0;
  
  return ratio;
}

float getSmokePPM(float ratio) {
  float a = -0.37958; 
  float b = 1.40301; 
  
  float ppm = pow(10, (log10(ratio) - b) / a);
  
  return ppm;
}

int readFlameSensor(int pin){
    for (int i = 0; i < pollingTimes+1; i++) {
        flameReading[i] = analogRead(pin);
    }

    return getMedian(flameReading, pollingTimes);
}

void checkResetButton(){
    int resetButtonState = digitalRead(RESETPIN);

    if (resetButtonState == LOW) {
        if (!resetButtonPressed) {
            resetButtonPressed = true;
            resetButtonPressTime = millis();
        }
    } else {
        if (resetButtonPressed) {
            if (millis() - resetButtonPressTime >= resetButtonTime) {
                resetLittleFS();
            }
            
            resetButtonPressed = false;
        }
    }
}

void readDHT(unsigned long currentMillis){
    if (currentMillis - prevMillisDHT >= 1000) {
        humidity = dht.getHumidity();
        temperature = dht.getTemperature();
        
        if(dht.getStatus() != 0){
            Serial.print("DHT11 Error :");
            Serial.println(dht.getStatusString());
        }else{
            Serial.print("Humidity: ");
            Serial.print(humidity);
            Serial.print(" %\t");
            Serial.print("Temperature: ");
            Serial.print(temperature);
            Serial.println(" C");
        }
        prevMillisDHT = currentMillis;
    }
}

bool checkFlame(){
    return (flameValue < flameThreshold || flameValue2 < flameThreshold);
}

bool checkSmoke(){
    return (smokePPM > smokePPMThreshold && millis() > burnInTime);
}

bool checkTemp(){
    return (temperature > tempThreshold && !isnan(temperature));
}

void loop() {
    unsigned long currentMillis = millis();

    checkResetButton();

    smokeValue = readSmokeSensor();

    float ratio = calculateRatio(smokeValue);
    smokePPM = getSmokePPM(ratio);

    flameValue = readFlameSensor(KY026PIN);
    flameValue2 = readFlameSensor(KY026PIN_2);
    readDHT(currentMillis);

    if (currentMillis - prevMillis >= 1000) {
        Serial.print("Smoke Value: ");
        Serial.println(smokeValue);
        Serial.print("Smoke PPM: ");
        Serial.println(smokePPM);
        Serial.print("ratio: Rs/R0 ");
        Serial.println(ratio);
        Serial.print("Flame Value: ");
        Serial.println(flameValue);
        Serial.print("Flame2 Value: ");
        Serial.println(flameValue2);

        if (warningKebakaran) {
            Serial.println("Warning: Kebakaran detected!");
        }

        prevMillis = currentMillis;
    }

    int currentState = checkFlame() + checkSmoke() + checkTemp();

    if (currentState >= 2) {
        
        if (!warningKebakaran) {
            KebakaranMillis = currentMillis;
            Serial.println("Kebakaran detected!");
        }

        KebakaranEndMillis = currentMillis;
        warningKebakaran = true;
    } else {
        if (warningKebakaran && currentMillis - KebakaranEndMillis >= KebakaranEndLastingTime) {
            warningKebakaran = false;
            sendNotificationKebakaran = false;
            Serial.println("Kebakaran ended!");
        }else{
            KebakaranMillis = currentMillis;
        }
    }

    if (warningKebakaran){
        if (currentMillis - prevMillisBuzzer >= 1000) {
            digitalWrite(BUZZERPIN, !digitalRead(BUZZERPIN));
            prevMillisBuzzer = currentMillis;
        }
    }else{
        digitalWrite(BUZZERPIN, LOW);
    }

    if (openedServo && servoState && !warningKebakaran) {
        Serial.println("Closing servo...");
        openedServo = false;
        closeServo();
    }
    
    if (!servoState && !openedServo && warningKebakaran && currentMillis - KebakaranMillis >= KebakaranLastingTime) {
        Serial.println("Opening servo for flame...");
        openedServo = true;
        openServo();
    }

    if (WiFi.status() != WL_CONNECTED) {
        digitalWrite(BUILTIN_LED, LOW);

        if (!setupFailed) {
            return;
        }

        if (SerialBT.isReady()) {
            bluetoothState();
        } else {
            Serial.println("WiFi not connected!\nbluetooth state");
            SerialBT.begin(device_name);
            delay(1000);
        }
    } else {
        if (!digitalRead(BUILTIN_LED))
            digitalWrite(BUILTIN_LED, HIGH);

        if (setupFailed) {
            Serial.println("WiFi connected!. Restarting...");
            ESP.restart();
        }

        JWT.loop(app.getAuth());
        app.loop();
        Database.loop();
        messaging.loop();

        if (getServoState != servoState) {
            if (servoState){
                Database.set<bool>(aClient2, "/uid/" + String(owner_uid) + "/devices/" + device_name + "/servo", true, asyncCB, "servoTask");
                getServoState = true;
            } else {
                Database.set<bool>(aClient2, "/uid/" + String(owner_uid) + "/devices/" + device_name + "/servo", false, asyncCB, "servoTask");
                getServoState = false;
            }
            aServoTask.clear();
        }

        if (aServoTask.available()) {
            RealtimeDatabaseResult &RTDB = aServoTask.to<RealtimeDatabaseResult>();
            getServoState = RTDB.to<bool>();

            Serial.println("Servo state:");
            Serial.println(getServoState);

            if (getServoState != servoState) {
                if (getServoState) {
                    Serial.println("Opening servo...");
                    openServo();
                } else {
                    Serial.println("Closing servo...");
                    closeServo();
                }
            }
        }

        if (app.ready()) {
            if (warningKebakaran && !sendNotificationKebakaran) {
                sendNotification("Kebakaran", "Kebakaran terdeteksi di " + device_name, sendNotificationKebakaran);
            }

            if (currentMillis - lastUpdateMillis >= updateMillis) {
                lastUpdateMillis = currentMillis;
                epochTime = getTime();
                Serial.println("Updating...");
                Serial.println(epochTime);
                printLocalTime();

                if (lastHumidity != humidity && !isnan(humidity) || lastTemperature != temperature && !isnan(temperature)) {
                    lastHumidity = humidity;
                    lastTemperature = temperature;

                    Database.set<float>(aClient2, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/humidity", humidity, asyncCB, "humidityTask");
                    Database.set<float>(aClient2, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/temperature", temperature, asyncCB, "temperatureTask");
                }
                
                int flamePercentage = reverseMap(flameValue, 0, 4095, 100, 0);
                int flame2Percentage = reverseMap(flameValue2, 0, 4095, 100, 0);

                if (flamePercentage < flame2Percentage) {
                    flamePercentage = flame2Percentage;
                }

                Database.set<int>(aClient2, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/smoke", smokePPM, asyncCB, "smokeTask");

                Database.set<int>(aClient2, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/flame", flamePercentage, asyncCB, "flameTask");

                Database.set<int>(aClient2, "/uid/"+ String(owner_uid) +"/devices/"+ device_name +"/last_update", epochTime, asyncCB, "lastUpdateTask");
            }
        }  
    }
}


long reverseMap(long y, long out_min, long out_max, long in_min, long in_max) {
  return (y - out_min) * (in_max - in_min) / (out_max - out_min) + in_min;
}

void resetLittleFS() {
    Serial.println("Formatting LittleFS...");
    if(LittleFS.format()){
        Serial.println("LittleFS formatted successfully");
    } else {
        Serial.println("An error occurred while formatting LittleFS");
    }
    delay(1000);
    ESP.restart();
}

void closeServo() {
    for (int i = pos_buka; i >= pos_tutup; i--) {
        myservo.write(i);
        delay(5);
    }
    myservo.write(pos_tutup+5);
    servoState = false;
}

void openServo() {
    for (int i = pos_tutup; i <= pos_buka; i++) {
        myservo.write(i);
        delay(5);
    }
    myservo.write(pos_buka-5);
    servoState = true;
}

unsigned long getTime() {
  time_t now;
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    return(0);
  }
  time(&now);
  return now;
}

void printLocalTime() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    Serial.println("Failed to obtain time");
    return;
  }
  Serial.println(&timeinfo, "Time: %A, %B %d %Y %H:%M:%S");
}

void bluetoothState() {

    if (SerialBT.available()) {
        bt_recv_buffer = SerialBT.readStringUntil('\n');
        Serial.println(bt_recv_buffer);

        DeserializationError error = deserializeJson(jsonDocRecv, bt_recv_buffer);
        if (error) {
            Serial.print("deserializeJson() failed: ");
            Serial.println(error.c_str());
            return;
        }

        const char* type = jsonDocRecv["type"];
        Serial.println(type);

        if (type == nullptr) {
            Serial.println("Type not found!");
            return;
        }

        if (strcmp(type, "DEVICEID") == 0) {
            Serial.println("Start of communication");

            jsonDocSend.clear();
            jsonDocSend["device_id"] = device_name;

            serializeJson(jsonDocSend, bt_send_buffer);
            Serial.println(bt_send_buffer);
            SerialBT.println(bt_send_buffer);

        } else if (strcmp(type, "CONNECT") == 0) {

            if (jsonDocRecv["ssid"] == nullptr || jsonDocRecv["password"] == nullptr) {
                Serial.println("SSID or password not found!");

                jsonDocSend.clear();
                jsonDocSend["status"] = "FAIL";

                serializeJson(jsonDocSend, bt_send_buffer);
                SerialBT.println(bt_send_buffer);
                return;
            }

            ssid = strdup(jsonDocRecv["ssid"]);
            password = strdup(jsonDocRecv["password"]);

            Serial.printf("Connecting to %s\n", ssid);

            WiFi.begin(ssid, password);
            
            for (int i = 0; i < 15; i++)
            {
                if (WiFi.status() == WL_CONNECTED){
                    break;
                }else{
                    Serial.print(".");
                    delay(1000);
                }
            }

            if (WiFi.status() != WL_CONNECTED) {
                Serial.println("Connection Failed!");

                jsonDocSend.clear();
                jsonDocSend["status"] = "FAIL";

                serializeJson(jsonDocSend, bt_send_buffer);
                SerialBT.println(bt_send_buffer);
                return;
            }

            Serial.println("Connected to the WiFi network.");

            IPAddress localIP = WiFi.localIP();
            Serial.print("IP Address: ");
            Serial.println(localIP);

            if (owner_uid == nullptr) {
                WiFi.disconnect();

                Serial.println("Owner UID not found! Fail to setup");
                return;
            }

            writeFile(LittleFS, "/ssid", ssid);
            writeFile(LittleFS, "/password", password);
            writeFile(LittleFS, "/owner_uid", owner_uid);

            delay(1000);
            ESP.restart();

        } else if (strcmp(type, "UID") == 0) {
            Serial.println("Setting owner UID...");
            if (owner_uid != nullptr) {
                Serial.println("Owner UID already set!");

                jsonDocSend.clear();
                jsonDocSend["status"] = "FAIL";

                serializeJson(jsonDocSend, bt_send_buffer);
                SerialBT.println(bt_send_buffer);
                return;
            }

            owner_uid = strdup(jsonDocRecv["uid"]);

            jsonDocSend.clear();
            jsonDocSend["status"] = "OK";

            serializeJson(jsonDocSend, bt_send_buffer);
            SerialBT.println(bt_send_buffer);

        } else if (strcmp(type, "SCAN") == 0) {
            Serial.println("Scanning WiFi networks...");

            int n = WiFi.scanNetworks();

            Serial.println("Scan done");
            jsonDocSend.clear();
            JsonArray networks = jsonDocSend.createNestedArray("networks");
            
            if (n == 0 || n == -2) {
                Serial.println(n == 0 ? "No networks found" : "Failed to scan networks");
            } else {
                Serial.print(n);
                Serial.println(" networks found");
                for (int i = 0; i < n; ++i) {
                    JsonObject network = networks.createNestedObject();
                    network["name"] = WiFi.SSID(i);
                    network["strength"] = WiFi.RSSI(i);
                    network["password"] = WiFi.encryptionType(i) != WIFI_AUTH_OPEN;
                    Serial.print(i + 1);
                    Serial.print(": ");
                    Serial.print(WiFi.SSID(i));
                    Serial.print(", ");
                    Serial.print(WiFi.RSSI(i));
                    Serial.print(" dBm");
                    Serial.print(", ");
                    Serial.println(WiFi.encryptionType(i) == WIFI_AUTH_OPEN ? "Open" : "Password protected");
                }
            }
            serializeJson(jsonDocSend, bt_send_buffer);
            SerialBT.println(bt_send_buffer);
        }
    }
}

void sendNotification(String title, String body, bool &isSent) {
    Serial.println("send notification");
    String fcmGet = Database.get<String>(aClient, "/uid/"+ String(owner_uid) +"/fcmtokens");

    if (aClient.lastError().code() != 0) {
        Serial.println("Failed to get FCM Token");
        return;
    }

    Serial.println("FCM Token:");
    Serial.println(fcmGet);

    StaticJsonDocument<512> jsonDocOut;
    DeserializationError error = deserializeJson(jsonDocOut, fcmGet);

    if (error) {
        Serial.print("deserializeJson() failed: ");
        Serial.println(error.c_str());
        return;
    }

    JsonObject data = jsonDocOut.as<JsonObject>();
    for (JsonPair kv : data) {
        const char* fcmToken = kv.key().c_str();

        Messages::Message msg;
        getMsg(msg, fcmToken, title, body);
        messaging.send(aClient, Messages::Parent(FIREBASE_PROJECT_ID), msg, aResult1);
    }

    isSent = true;
}

void getMsg(Messages::Message &msg,const String token, const String title, const String body) {
    msg.token(token);

    Messages::Notification notification;
    notification.body(body).title(title);

    object_t data, obj1;
    JsonWriter writer;

    writer.create(obj1, "device_id", device_name.c_str());
    writer.join(data, 1, obj1);
    msg.data(data);
    data.clear();

    Messages::AndroidConfig androidConfig;
    androidConfig.priority(Messages::AndroidMessagePriority::_HIGH);
    Messages::AndroidNotification androidNotification;
    androidNotification.notification_priority(Messages::NotificationPriority::PRIORITY_HIGH);
    androidConfig.notification(androidNotification);
    msg.android(androidConfig);
    msg.notification(notification);
}

void timeStatusCB(uint32_t &ts) {
    if (time(nullptr) < FIREBASE_DEFAULT_TS) {
        while (time(nullptr) < FIREBASE_DEFAULT_TS) {
            delay(100);
        }
    }
    ts = time(nullptr);
}

void asyncCB(AsyncResult &aResult) {
    printResult(aResult);
}

void printResult(AsyncResult &aResult) {
    if (aResult.isEvent()) {
        Firebase.printf("Event task: %s, msg: %s, code: %d\n", aResult.uid().c_str(), aResult.appEvent().message().c_str(), aResult.appEvent().code());
    }
    if (aResult.isError()) {
        Firebase.printf("Error task: %s, msg: %s, code: %d\n", aResult.uid().c_str(), aResult.error().message().c_str(), aResult.error().code());
    }
    if (aResult.available()) {
        Serial.println("----------------------------");
        Firebase.printf("task: %s, payload: %s\n", aResult.uid().c_str(), aResult.c_str());
    }
}