# Hubitat-Sunsa-Wands

This is a Hubitat driver for Sunsa Wands.  

See https://www.sunsahomes.com/ for more information on this product.

In order to use this driver, you need two things.  First is an "api-key" which
you can create in the Sunda Wands mobile app. Second is your user id.  You won't
find this in the current version of the app, so you'll have to email Sunsa for it.

After you have your userid and an api key, go to the Devices section in Hubitat
and click on the "Add Virtual Device" button at the top right.

This device is responsible for communicating with the Sunsa Wands API and will
create a child "Sunsa Wand" device for each device in your Sunsa account.

In the Device Name field, enter something like "Blinds" or "Window Blinds",
or whatever you want.  In the Type field, select "Sunsa Wands API".  Click Save Device.

Now, enter your Sunsa user id and api key and click "Save Preferences"
The driver will connect to the Sunsa Wands API and create a child
Sunsa Wand driver for each wand associated with your user id.

Each child device has a single preference you can configure: Close Direction.  This
controls what direction the blinds are closed when the Close command is set.  The
default is "Up".

The SetTiltLevel command takes a value from -100 to 100, -100 being closed upwards 0 is fully open
and 100 is closed downwards.  You can also specify any value in between to partially open.  The
Sunsa Wand only allows tilting in increments of 10 so any value will be rounded down or up to the
nearest 10 by the API.

The SetPosition, StartPositionChange and StopPositionChange commands are not supported since they
are not applicable to this device.

If no child devices are created, double check your user id and api key,
and also check the Hubitat log.

Comments, suggestions, bugs, etc. please send me an email.  dcaton1220@gmail.com

This driver is based on version 1.0.4 of the Sunsa Wand API, which can be found here: 
https://app.swaggerhub.com/apis-docs/Sunsa/Sunsa/1.0.4

Release History
===============

v 1.0.1    09/20/21

    Temporarialy added WIndowShade capability to child driver to work around
    Window Blind not showing up in Rule Manager capabilities list.

v 1.0.0    09/19/21

   Known issues:

   1. The API does not currently return the value of the temperature or light sensors in the wand.

   2. There is a typo in the returned json from the PUT api: "idDevice" is misspelled "idDevivce".
      When the api is fixed I will update the driver accordingly.

   3. The API does not currently have any way of notifying when the state of a wand changes 
      (e.g. from the mobile app or via the Alexa integration) so the current state shown in Hubitat
      may not be the actual state.  I intend to add an optional polling mechanism which will
      periodically query the Sunsa API to keep Hubitat's state somewhat in sync.
