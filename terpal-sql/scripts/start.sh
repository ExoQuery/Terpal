#!/bin/bash

docker-compose down && docker-compose run --rm --service-ports setup
