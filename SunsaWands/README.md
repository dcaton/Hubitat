# Hubitat-Sunsa-Wands

This is a Hubitat driver for Sunsa Wands.  

See https://www.sunsahomes.com/ for more information on this product.

In order to use this driver, you need two things.  First is an api-key which you can create in the Sunda Wands mobile app.
Second is your user id.  You won't find this in the current version of the app, so you'll have to email Sunsa for it.

After you have your userid and an api key, create an instance of the Sunsa Wands driver and enter your userid and api key.
The driver will create a child Sunsa Wand driver for each wand associated with your user id.

Comments, suggestions, bugs, etc. please send me an email.  dcaton1220@gmail.com

This driver is based on version 1.0.4 of the Sunsa Wand API, which can be found here: 
https://app.swaggerhub.com/apis-docs/Sunsa/Sunsa/1.0.4

Release History
===============
v 1.0.0    09/18/21

   Known issues:

   1. The API does not currently return the value of the temperature sensor in the wand (the app does report this).

   2. There is a typo in the returned json from the PUT api: "idDevice" is misspelled "idDevivce".  When the api is
      fixed I will update the driver accordingly.

   3. The API does not currently have any way of notifying when the state of a wand changes (e.g. from the mobile app
      or via the Alexa integration) so the current state shown in Hubitat may not be the actual state.  I intend to 
      add an optional polling mechanism which will periodically query the Sunsa API to keep Hubitat's state somewhat 
      in sync.

   4. Hubitat package manager is not yet supported.