from flask import Flask, request, make_response, jsonify

import googlemaps
import polyline
import pandas as pd
from datetime import datetime

from selenium import webdriver

# Start Selenium
driver = webdriver.Chrome()
driver.implicitly_wait(1)

gmaps = googlemaps.Client(key='***REMOVED***')

app = Flask(__name__)


def to_state_coords(coords):
    # Unwrap Tuple
    lat, long = coords

    # Load the site
    driver.get("http://www.earthpoint.us/StatePlane.aspx")

    # Input output format
    driver.find_element_by_id("ContentPlaceHolder1_Zone2").send_keys("3104")

    # Input latitude
    driver.find_element_by_id("ContentPlaceHolder1_Latitude").send_keys(str(lat))

    # Input longitude
    driver.find_element_by_id("ContentPlaceHolder1_Longitude").send_keys(str(long))

    # Press the button
    driver.find_element_by_id("ContentPlaceHolder1_btnLatLonCalc_Button1").click()

    # Get output
    content = driver.find_element_by_css_selector(
        'div > table > tbody > tr > td > table > tbody > tr:nth-child(9) > td:nth-child(2)')
    result = content.text

    x = result[5:result.index('ftUSE')]
    y = result[result.index('ftUSE') + 6:result.index('ftUSN')]

    return float(x), float(y)


def weight_path(path, radius=5):
    # Convert to coords
    path = [to_state_coords(coords) for coords in path]
    print("CONVERTED PATH")
    print(path)

    # Read data
    df = pd.read_csv('felony_data.csv')
    total_crime = 0

    for coords in path:
        x, y = coords
        res = df[
            df['X_COORD_CD'].between(x - radius, x + radius) &
            df['Y_COORD_CD'].between(y - radius, y + radius)
            ]
        crime = len(res)
        total_crime += crime

    avg_crime = total_crime / len(path)
    print(f'{total_crime} -> {avg_crime}')
    return avg_crime


@app.route('/find_path')
def find_safest_path():
    location = request.args.get('location')
    destination = request.args.get('destination')

    # Get directions from Google Maps API
    now = datetime.now()
    directions_result = gmaps.directions(location,
                                         destination,
                                         mode="walking",
                                         departure_time=now,
                                         alternatives=True)

    print(f"Routes returned by Google: {len(directions_result)}")

    # Get the encoded polyline paths
    encoded_paths = [route.get('overview_polyline').get('points') for route in directions_result]
    print(encoded_paths)

    # Decode the polyline paths
    decoded_paths = [polyline.decode(path) for path in encoded_paths]
    print("LAT/LONG PATH")
    print(decoded_paths)

    # Get the weights based on crime data
    weights = [weight_path(path) for path in decoded_paths]
    print(weights)

    # Pick the best path (lowest weight)
    best_path = weights.index(min(weights))
    print(best_path)

    return make_response(jsonify(best_path=encoded_paths[best_path]), 200)
