---
layout: page
position: 2
section: home
title:  "Getting Started"
---

# Getting Started

FoundaML will help you industrialize your machine learning projects. It sits between your clients (web apps, mobile apps etc) and your algorithms (heuristics or machine learning).

![hello](img/Foundaml.png)

FoundaML has four key concepts.

### Projects
A project is a set of algorithms working on the same data. 
The principle here is that you can compare and switch algorithms only if they operate on the same data.

When you use FoundaML, you begin by defining your project with the data that it will work on and the objective it will pursue. For the moment, FoundaML supports the following types of problems.

* Classification

FoundaML supports a set of generic features that you can combine to build any algorithm, those include.

* Double or Float
* Integer
* String

### Algorithms

An algorithm is code, running on another instance, that compute values from data. Algorithms in the same project will work on the same data, that can be reprocessed in a pre processing pipeline ([An example with Tensorflow Transform](https://github.com/tensorflow/transform))
FoundaML can implement various backends. Currently, FoundaML supports the following APIs.

* TensorFlow Serving API

To perform the transformation from the project features to the algorithm input features, FoundaML requires features transformers. The same operation is required when converting the algorithm labels to the project labels, using label transformers.


### Predictions

A prediction belongs to a project. The algorithm that is executed depends on the project policy. You can choose between various policies available.

* **No Algorithm** (By default, a project will deny predictions until an algorithm is created)
* **DefaultAlgorithm** means executing the same algorithm all the time
* **RoundRobin** allows you to specify weights for each algorithms that you created in your projects. This is helpful for AB testing.


### Examples
Each prediction comes with a set of urls that allow you to tag it as correct or incorrect. This will help you generate a labeled dataset as well as evaluate your algorithm in real time.