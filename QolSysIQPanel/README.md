# Hubitat-QolSysIQPanel

This is a set of Hubitat Elevation drivers for the QolSys IQ Alarm Panel.  

See https://qolsys.com/products/ for more information on this panel.

Using these drivers, you can monitor the state of all the devices connected
to your panel, such as door/window contacts, motion detectors, smoke and CO
detectors, water sensors, etc., as well as the state of the system itself 
(disarmed, armed, alarm, etc.).  You can also arm and disarm the system.

This has only been tested with the IQ2+ panel, although it probably works
with the IQ2 and the new IQ4 panels.

Please note that although these panels have z-wave controllers, there is
no integration with any z-wave devices, as the interface these drivers use
does not provide that capability.  


**WARNING**
=======

These drivers provide the ability to arm and disarm your alarm system.
If you enable that ability, take appropriate measures to secure your Hubutat hub
and any other systems (NodeRed, Webcore, etc.) where you create rules that contain
any alarm system user codes.

Although these drivers use a supported alarm system interface, the interface
itself is not publicly documented, and these drivers heavily rely on reverse
engineering to do what they do.  Use at your own risk.

Prerequisites
=============

1. You must have the dealer code for your panel in order to enable the 3rd
Party Connection option.  The default dealer code is 2222.  If your panel was
installed by an alarm installer, they may have changed the dealer code and you
will need to obtain it from them.  Please note that the "dealer" code is NOT the
same as the "installer" code, nor it is the same as any user codes.

2. Your QolSysIQ2+ panel must be running system firmware 2.4.0 or later.  Instructions
on how to check your firmware version and install updates may be found here:
https://www.alarmgrid.com/downloads/qolsys-iqpanel2plus-firmware-updates
I have no affiliation with AlarmGrid, but they are a good source of information, are
very DIY friendly, and generally have very competitive pricing, so give them a look
if you need any alarm system devices.

3. You must have Wifi enabled on your alarm panel, and the IP address of your alarm
panel must be reachable from your HE hub.  If Wifi isn't enabled, on your alarm panel, touch the small gray bar at the top of the screen, then choose Settings -> Advanced Settings -> Wifi and configure the Wifi settings.  

If your alarm panel is configured to use DHCP (the default), it is strongly suggested that you go into your router or DHCP server and reserve the IP address assigned to your alarm panel, so that it cannot change in the future.  Alternatively, you can assign a
static IP address to the alarm panel.  Go into the Wifi setup screen, add a new Wifi connection and click the Advanced Settings checkbox in the Wifi parameters dialog.

4. If your alarm system is monitored, you may want to call the monitoring station and put your system in test mode for some period of time.  That way you can play with the various
commands without having the police or fire department show up at your door.

Installation Instructions
=========================

1. On your alarm panel, touch the small gray bar at the top of the screen, then
choose Settings -> Advanced Settings -> Enter Dealer Code -> Installation
-> Devices -> WiFi Devices -> 3rd Party Connections.  Check the box for Control4.
Confirm Ok, and wait for the panel to reboot.

2. Repeat step 1, but this time when you get to the 3rd Party Connections screen,
touch "Reveal Secure Token".  Write down the 6 character token and make sure to
keep it secure.  Touch the icon of a house at the bottom of the screen when finished.

3. Obtain the IP address of your alarm panel.  On the alarm panel, swipe left or
right until you see the Wifi / Software Update / Bluetooth screen.  Touch the "Details" button under Wifi and make a note of the panel's IP address.

4. Install the HE drivers.  The easiest way to do this and keep up to date is to use
the Hubitat Package Manager app.

5. On the HE Devices page, click on the Add Device button in the upper right corner, then
click the "Virtual" button.  In the Device Name field enter something like "Alarm System".
In the Type field select "QolSys IQ Alarm Panel".  Click "Save Device".

6. Under preferences, enter the IP address and the access token you obtained in steps 2 and 3.  

7. There are three additional settings that will cause HE's mode to be changed when the alarm
panel's arming status changes.  For example, when the alarm system is disarmed you may choose
to have HE's mode set to "Home", armed-stay to "Night" and armed-away to "Away".  If you have 
created or modified HE's default modes, they will appear in the list.  You may leave any or all
of these settings to "No selection", in which case no change to HE's current mode will occur
when the alarm system's arming mode chages to that particular state.

8. If you want to allow the alarm system to be armed or disarmed from HE, enable the "Allow HE to send arming and disarming commands" setting.  If this setting is turned off, the ArmStay, ArmAway and Disarm commands will have no effect.

9. If youi want to allow an alarm condition to be initiated from HE, enable the "Allow HE to trigger an alarm condition" setting.  If this setting is turned off, the Alarm command will have no effect.

8. Click Save Preferences, then go back to the HE Devices page.  If everything is configured
correctly, you should see an entry for your alarm system, plus a child device for every sensor
connected to the system.  The child devices will be given the same name as the corresponding
physical devices in the alarm system, but you can go into the device settings and change the
name if you want.

Events
======

All alarm system events cause an attribute change event to occur in the corresponding virtual
device in HE.  For example, opening a door or window will cause the corresponding HE device
attribute "contact" to change from "closed" to "open".

Arming, disarming or tripping the alarm system will cause one of the "Alarm_Mode_Partition_x"
attributes in the parent driver to change to one of the following enum values:

- DISARM
- ARM_STAY
- ARM_AWAY
- EXIT_DELAY
- ENTRY_DELAY
- ALARM_INTRUSION
- ALARM_AUXILIARY
- ALARM_FIRE
- ALARM_POLICE

The QolSys IQ panel has a feature called "partitions".  When enabled, up to four partitions may
be configured.  Every alarm sensor belongs to a partition, and each parition is armed and diarmed
individually.  A house with a deteched garage for example could use partition 0 for the house and
partition 1 for the garage.  Most residential systems will not have partitions enabled so all devices
are in partition 0, and the alarm system status is set in the "Alarm_Mode_Partition_0" attribute.

Commands
========

The parent driver has commands to arm and disarm the system.  These commands will only work if a
valid alarm panel access token has been set.  If you have no need for HE to arm or disarm your
system, leave the access token setting blank.  This is the safest way to configure the driver,
as it will effecively be in "reporting only" mode and cannot change the alarm system's armed state.

If you do want to control your alarm system via HE, please insure that your hub is sufficiently 
protected.  You will also need a valid alarm system user code to disarm (and possibly arm) the system.
There is nothing stopping you from hard coding an alarm system user code into a HE rule (or in Webcore,
NodeRed, etc.).  If you do that, make sure those systems are as secure as possible.

The armAway and armStay commands have a 'bypass' parameter.  If set to Yes, any alarm zones (devices) in
an open, tampered or faulted state will automatically be bypassed.  

The armAway command has a 'exitDelay' parameter.  This is the length of time in seconds that you may open
any entry/exit doors before the alarm is armed.

All arm and disarm commands have a userCode parameter.  A valid user code must be supplied to disarm the system.
If your alarm system is not configured for "secure arming", a user code is not required to arm the system.  In
the Device Details table for the driver, in the Data section the "Secure_Arm_Partition_n" value indicates
the secure arming setting for each partition.

If you add, remove or change any devices on your alarm system, simply click Refresh in the parent driver to update
the child drivers.

The child devices do not have any commands, they simply mirror the state of their respective physical devices
connected to the alarm system.  Generally there will be a single attribute such as "contact" or "motion" that
mirrors the state of the physical device.


Known issues
============

   1. Although there is a "tamper" value that is transmitted by the alarm panel, this value does not change even when
   the physical device is in a tampered state.  Therefore the "TamperAlert" capability has not been implemented in any
   of these drivers.  Perhaps a future alarm panel firmware update will enable tamper reporting.

   2. I do not have every possible type of device that could be connected to this alarm panel, so there may
   be devices that are not properly detected.  If a device on your alarm system does not have a corresponding virtual
   device in HE, please turn on all the logging options for the parent driver, click Refresh, email or post the relevant
   log entries in the HE community forum, and I will add support for the device.




Release History
===============

v 1.0.0    09/19/21

   Initial public version