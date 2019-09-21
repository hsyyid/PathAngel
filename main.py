from flask import make_response, jsonify

import googlemaps
from datetime import datetime

gmaps = googlemaps.Client(key='***REMOVED***')


def get_path(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """

    # GET DIRECTIONS FROM GOOGLE CLOUD
    request_json = request.get_json()
    location = request_json['location']
    destination = request_json['destination']

    now = datetime.now()
    directions_result = gmaps.directions(location,
                                         destination,
                                         mode="walking",
                                         departure_time=now,
                                         alternatives=True)

    # TODO: Decode directions result

    # WEIGHT DIRECTIONS USING CRIME DATA
    # RETURN BEST PATH

    return make_response(jsonify(message='Done'), 200)


if __name__ == "__main__":
    from flask import Flask, request

    app = Flask(__name__)


    @app.route('/')
    def index():
        return get_path(request)


    app.run('127.0.0.1', 8000, debug=True)
