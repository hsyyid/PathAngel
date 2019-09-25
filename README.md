# PathAngel
Source: [API](https://github.com/hsyyid/PathAngel) | [App](https://github.com/hsyyid/PathAngel/tree/app)

PathAngel finds the safest walking path for users by analyzing Google Maps suggested walking routes against location-based crime history. Internally, we generate a crime rating for each path using a historic crime dataset for New York City to weigh and compare paths.

Created at [BigRed//Hacks vol.7](https://twitter.com/bigredhacks) by [Hassan Syyid](https://github.com/hsyyid), [Sandeep Ramesh](https://github.com/F28L), and [Sandesh Mysore](https://github.com/Smy24).

## Technology

PathAngel is a native Android App written in Java, with a Python backend running on Flask.

Using historical crime data from the public NYC Open Data database, we use Pandas to weigh the walking paths provided by Google Maps Directions API and choose the safest one.

In order to speed up these computations, we switch from using Latitude/Longitude to the standard New York State coordinate system. To do this, we leverage Selenium in tandem with Pandas to both calculate and aggregate this information.

We also relied heavily on the Google Maps API to power the frontend experience (e.g. displaying the map, drawing the walking path, etc.)

## Links

* [Devpost Project](https://devpost.com/software/pathangel)
* [Demo](https://youtu.be/qpelhymdZ5o)
