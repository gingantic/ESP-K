# ESP-K (ESP32 Fire Detection System). My Final Project

A comprehensive IoT-based fire detection system that uses ESP32 microcontroller and Android mobile application for real-time monitoring and alerts.

## Overview

ESP-K is a fire detection system that monitors environmental conditions using multiple sensors and provides real-time notifications through a mobile application. The system is designed to detect potential fire hazards by monitoring:

- Temperature (using DHT sensor)
- Smoke levels (using MQ2 sensor)
- Flame detection (using KY026 sensors)
- Humidity levels

## Features

- **Multi-sensor Detection**: Combines data from multiple sensors for accurate fire detection
- **Real-time Monitoring**: Android app provides live updates of sensor readings
- **Push Notifications**: Instant alerts when fire hazards are detected
- **Automated Response**: Includes servo control for automated ventilation/sprinkler system
- **Bluetooth Configuration**: Easy device setup through Bluetooth pairing
- **Firebase Integration**: Real-time data synchronization and user authentication
- **Offline Capability**: Device continues monitoring even without internet connection

## Components

### Hardware
- ESP32 Microcontroller
- DHT Temperature & Humidity Sensor
- MQ2 Smoke Sensor
- KY026 Flame Sensors (2x)
- Servo Motor
- Buzzer for local alerts

### Software
- Android Mobile Application
  - Real-time sensor monitoring
  - Device management
  - User authentication
  - Push notifications
- ESP32 Firmware
  - Sensor data collection
  - Firebase integration
  - Bluetooth configuration
  - Automated response system

## Technical Stack

- **Mobile App**: Kotlin, Android SDK
- **Backend**: Firebase (Authentication, Realtime Database, Cloud Messaging)
- **IoT**: ESP32, Arduino Framework
- **Communication**: Bluetooth Serial, WiFi, Firebase RTDB

## Security Features

- User authentication
- Secure Firebase communication
- Bluetooth pairing protection
- Device-specific authentication