# Hyperplan Server
[![Build Status](https://travis-ci.org/scoverage/sbt-scoverage.png?branch=master)](https://travis-ci.org/hyperplan/server)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

![The interface between data scientists and developers](https://hyperplan.io/assets/img/illustrations/illustration-2.png)

Hyperplan is the interface between data scientists and developers. It is a proxy service that will help you to write machine learning pipelines. It provides:

* Projects, features and labels through its own data stores
* Algorithm selection and execution, compatible with TensorFlow, Rasa Nlu and more to come
* An API to execute and label data
* Data storage and distribution through many message bus providers such as Apache Kafka, Google PubSub or AWS Kinesis.

**Useful links**

[Hyperplan website](https://hyperplan.io

[Generating data with Hyperplan](https://medium.com/@sauray.antoine/data-generation-for-machine-learning-using-foundaml-5e324e6939f5)


## Building
`
sbt stage
docker-compose build
docker-compose up
`
