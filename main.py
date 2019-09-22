from flask import Flask, request, make_response, jsonify

import googlemaps
import polyline
import pandas as pd
from datetime import datetime
import os
import time
from uuid import uuid4

from selenium import webdriver
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions
from selenium.webdriver.common.by import By

# Start Selenium
driver = webdriver.Chrome()
driver.implicitly_wait(3)

gmaps = googlemaps.Client(key='***REMOVED***')

app = Flask(__name__)


def to_state_coords(path):
    # Convert to DF
    df = pd.DataFrame(path, columns=['Latitude', 'Longitude'])
    df['ToStatePlaneZone'] = '3104'

    # Export to CSV
    uid = str(uuid4())
    df.to_csv(f'{uid}.csv', index=False)

    # Load the site
    driver.get("https://www.earthpoint.us/SignIn.aspx")

    # Login
    if len(driver.find_elements_by_id("ContentPlaceHolder1_SignInEmailAddress")) > 0:
        driver.find_element_by_id("ContentPlaceHolder1_SignInEmailAddress").send_keys("hsyyid@umd.edu")
        driver.find_element_by_id("ContentPlaceHolder1_SignInPassword").send_keys("J@vas0ft")
        driver.find_element_by_id("ContentPlaceHolder1_btnSignInSubmit_Button1").click()

        # Wait
        try:
            WebDriverWait(driver, 1).until(
                expected_conditions.presence_of_element_located(
                    (By.ID, 'btnIsSignedInSignOut')
                )
            )
        finally:
            return do_conversion(uid)
    else:
        return do_conversion(uid)


def do_conversion(uid):
    # Open BatchConvert page
    driver.find_element_by_css_selector("#PanelMenuBottom > ul > li:nth-child(5)").click()

    # Select the output to be FEET
    driver.find_element_by_css_selector('#ConvertToTable tbody > tr:nth-child(2) > td:nth-child(3)').click()

    # Input the path
    frame = driver.find_element_by_id('iframe1')
    driver.switch_to.frame(frame)
    driver.find_element_by_id("FileUpload1").send_keys(os.getcwd() + "/" + uid + ".csv")

    # Click the button
    driver.find_element_by_id("btnConvertSheet_Button1").click()

    # Wait for file to download
    time.sleep(3)

    # Get result
    result_df = pd.read_csv(f"/Users/hassansyyid/Downloads/{uid}_converted.csv")
    subset = result_df[['to Survey Feet East', 'to Survey Feet North']]

    return [tuple(x) for x in subset.values]


def weight_path(path, radius=125):
    # Read data
    df = pd.read_csv('felony_data.csv')
    total_crime = 0
    crime_locations = []

    for coords in path:
        x, y = coords

        res = df[
            df['X_COORD_CD'].between(x - radius, x + radius) &
            df['Y_COORD_CD'].between(y - radius, y + radius)
            ]
        crime = len(res)
        total_crime += crime

        # Keep track of the crime on our route for the heat map
        if crime > 0:
            subset = res[['Latitude', 'Longitude']]
            crime_locations += [tuple(x) for x in subset.values]

    avg_crime = total_crime / len(path)
    print(f'{total_crime} -> {avg_crime}')
    return avg_crime, crime_locations


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

    # Convert to NY State Plane Coords
    paths = [to_state_coords(path) for path in decoded_paths]
    print(paths)

    # Get the weights based on crime data
    weights, crime = zip(*[weight_path(path) for path in paths])
    print(weights)

    # Pick the best path (lowest weight)
    best_path = weights.index(min(weights))
    print(best_path)

    return make_response(jsonify(best_path=encoded_paths[best_path], heatmap=[j for i in crime for j in i]), 200)

# if __name__ == '__main__':
#     result_df = pd.read_csv(f"./e23a868d-d541-4401-8f4d-df76fde0f90a_converted.csv")
#     subset = result_df[['to Survey Feet East', 'to Survey Feet North']]
#
#     path = [tuple(x) for x in subset.values]
#     print(path)
#
#     weights = weight_path(path)
#     print(weights)
