# SPI on AndroidThings

## Describe
 Create an Android application running on Raspberry Pi 3 to:
 + Write blocks of data to an RFID card including
 + Continuously read an UID and the written data from an RFID card and display this information
on a screen.
 + Turn the green LED of an RGB LED on when the card of one of your group members is presented.
 + Flashing the red LED of an RGB LED 5 times in 2 seconds when the other cards are presented.
+ By default, the blue LED of an RGB LED is turned on, and only one LED is turned on at a time.

## GPIO ports

| Peripherals    |  Raspberry Pi 3 ports |
|----------------|-----------------------|
| LED RED        |  BCM2|
| LED GREEN      |  BCM3|
| LED BLUE       |  BCM4|
| SPI_PORT       |  SPI0.0|
| PIN_RESET      |  BCM25|


## Usage

+ Press Write Button to start writing data to RFID card. Name, ID, DOB will be written on RFID card.
+ Press Read Button to start reading data from RFID card. Name, ID, DOB will be displayed on screen. The RGB LED will blink with rule:
	+ The green LED of RGB LED is on when the card of one of group members is presented.
    + The red LED of RGB LED flashes 5 times in 2 seconds when the other cards are presented.
    + By default, the blue LED of RGB LED is on, and only one LED is turned on at a time.
	+ ID in the RFID card will be send to http://demo1.chipfc.com/SensorValue/List/7 after read
+ Press Stop Button to stop writing or reading.

## MEMBERS

+ Trần Minh Đức - 1610800
+ Trần Thanh Sang - 1612939
+ Nguyễn Minh Nhựt - 1612483
+ Lê Đức Trung - 1613786

