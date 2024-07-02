#!/bin/bash

docker-compose down && docker-compose build && docker-compose run --rm --service-ports setup
