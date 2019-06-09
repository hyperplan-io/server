# Hyperplan Server
[![Build Status](https://travis-ci.org/scoverage/sbt-scoverage.png?branch=master)](https://travis-ci.org/hyperplan/server)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Hyperplan is a service that help you manage your algorithms in a production environment. It's a great way to start industrializing your machine learning projects. It comes with three principles

* **Focus on the problem** machine learning is one solution, not the problem. Hyperplan asks you to specify a problem first
* **Data is the key**  Hyperplan helps you to build a pipeline to get your labeled data back from your users. You won't need to waste time hand labelling data. 
* **Iteration is crucial** you should start with a simple heuristic before training a big neural network. Hyperplan forces you to integrate your algorithms early in the process so you can start delivering results fast. Hyperplan also comes with built in data streaming and AB testing support.

**Useful links**

[Hyperplan website](https://hyperplanio.github.io/server/)

[Generating data with Hyperplan](https://medium.com/@sauray.antoine/data-generation-for-machine-learning-using-foundaml-5e324e6939f5)

# Backend
Hyperplan does not execute algorithms on its own. It relies on backends for this task. Currently it is compatible with
* TensorFlow serving API


## Building
`
sbt stage
docker-compose build
docker-compose up
`
