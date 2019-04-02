# Foundaml
Foundaml is a service that help you manage your algorithms in a production environment. It's a great way to start industrializing your machine learning projects. Foundaml comes with three principles

* **Focus on the problem** machine learning is one solution, not the problem. Foundaml asks you to specify a problem first
* **Data is the key**  Foundaml helps you to build a pipeline to get your labeled data back from your users. You won't need to waste time hand labelling data. 
* **Iteration is crucial** you should start with a simple heuristic before training a big neural network. Foundaml forces you to integrate your algorithms early in the process so you can start delivering results fast. Foundaml also comes with built in data streaming and AB testing support.

**Useful links**

[Foundaml website](https://foundaml.github.io/server/)

[Generating data with Foundaml](https://medium.com/@sauray.antoine/data-generation-for-machine-learning-using-foundaml-5e324e6939f5)

# Backend
Foundaml does not execute algorithms on its own. It relies on backends for this task. Currently it is compatible with
* TensorFlow serving API

[![Build Status](https://travis-ci.org/scoverage/sbt-scoverage.png?branch=master)](https://travis-ci.org/foundaml/server)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)


## Building
`
sbt stage
docker-compose build
docker-compose up
`
