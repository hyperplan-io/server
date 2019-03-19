#!/bin/bash
set -e

git config --global user.email "sauray.antoine@gmail.com"
git config --global user.name "antoinesauray"
git config --global push.default simple

sbt docs/publishMicrosite
