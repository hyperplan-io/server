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

An algorithm is code, running on another instance, that computes values from data. Algorithms in the same project will work on the same data. if necessary, they can reprocess  it in a pre processing pipeline ([An example with Tensorflow Transform](https://github.com/tensorflow/transform)). This can be useful if you need to normalize your data or you need to try different word embeddings on your  NLP problem.


FoundaML can implement various backends. Currently, FoundaML supports the following APIs.

* TensorFlow Serving API

To perform the transformation from the project features to the algorithm input features, FoundaML needs **features transformers**. The same operation is required when converting the algorithm labels to the project labels, using **label transformers**.


### Predictions

A prediction belongs to a project and an algorithm. The algorithm that is executed depends on the project policy (if not specified explicitely by the client). You can choose between various policies available.

* **No Algorithm** (By default, a project will deny predictions until an algorithm is created)
* **DefaultAlgorithm** means executing the same algorithm all the time
* **RoundRobin** allows you to specify weights for each algorithms that you created in your projects. This is helpful for AB testing.


### Examples
Each prediction comes with a set of urls that allow you to tag it as correct or incorrect. This will help you generate a labeled dataset as well as evaluate your algorithm in real time.

Let's now move on to the [Titanic Example](https://foundaml.github.io/server/the_titanic.html)