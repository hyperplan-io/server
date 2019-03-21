---
layout: page
position: 3
section: home
title:  "Example: The Titanic"
---

# The Titanic (Work in progress)
The Kaggle Titanic challenge is a popular Kaggle contest. It will serve as a good example to teach you how to use FoundaML to solve this problem.

# Creating the project
To solve this problem, we need to list the data on which our algorithms will perform. The features look as follows.

* PassengerId
* Survived
* Pclass
* Name
* Sex
* Age
* SibSp
* Parch
* Ticket
* Fare
* Cabin
* Embarked

The algorithm should predict whether or not the person survived. This is a classification problem. We can now create the project with a curl request.

```
curl -X POST \
  http://localhost:8080/projects/ \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
	"id": "kaggle-titanic",
    "name": "Kaggle Titanic",
    "configuration": {
        "problem": {
            "class": "Classification"
        },
        "features": {
        	"featuresClasses": [
        		{
        			"name": "passengerId",
        			"featureClass": "IntFeature",
        			"description": "The unique identifier of the passenger"
        		},
        		{
        			"name": "pClass",
        			"featureClass": "IntFeature",
        			"description": "Class of travel"
        		},
        		{
        			"name": "name",
        			"featureClass": "StringFeature",
        			"description": "Name of passenger"
        		},
        		{
        			"name": "sex",
        			"featureClass": "StringFeature",
        			"description": "Gender"
        		},
        		{
        			"name": "age",
        			"featureClass": "IntFeature",
        			"description": "Age"
        		},
        		{
        			"name": "sibSp",
        			"featureClass": "IntFeature",
        			"description": "Number of Sibling/Spouse aboard"
        		},
        		{
        			"name": "pArch",
        			"featureClass": "IntFeature",
        			"description": "Number of Parent/Child aboard"
        		},
        		{
        			"name": "ticket",
        			"featureClass": "StringFeature",
        			"description": "The ticket identifier"
        		},
        		{
        			"name": "fare",
        			"featureClass": "StringFeature",
        			"description": "Which fare"
        		},
        		{
        			"name": "cabin",
        		     "featureClass": "StringFeature",
        			"description": "Which cabin"
        		},
        		{
        			"name": "embarked",
        			"featureClass": "StringFeature",
        			"description": "The port in which a passenger has embarked. C - Cherbourg, S - Southampton, Q = Queenstown"
        		}
        	]
        },
        "labels": [
            "survived",
            "notSurvived"
        ]
    }
}'
```

# Our first algorithm, a simple heuristic
Our first algorithm will be quite simple. It will be simple heuristic.

```
if age > 18 and age < 40:
    return 'survived'
else:
    return 'did_not_survived'
```
It does not really matter at that point what we compute.

We will use the [TensorFlow Serving API](https://www.tensorflow.org/tfx/serving/api_rest) for our algorithm. I suggest you read about it before you continue this example.

A NodeJs implementation is available [here](https://antoinesauray.github.com/foundaml-server)


### Features transformation
TensorFlow Serving has a notion of signature name.  We also have feature names to map

```
"featuresTransformer": {
	"signatureName": "",
	"fields": [
		"passenger_id",
		"p_class",
		"name",
		"sex",
		"age",
		"sib_sp",
		"p_arch",
		"ticket",
		"fare",
		"cabin",
		"embarked"
	]
}
```
### Labels transformation
It is possible that the output of the algorithm does not exactly match the output of our project. We can define a transformation that maps one to the other. 
```
"labelsTransformer": {
    "function": "map",
    "fields": {
	    "survived": "survived",
	    "did_not_survived": "notSurvived"
	}
}
```
The keys of the ``field`` object are the outputs of the algorithm. Their value is the class of the project that we want to map it to.

In case they do match, you can use the `identity` function.

```
"labelsTransformer": {
   "function": "identity"
}
```

## Adding our algorithm to the project
We can summarize the information above with this http query.

```
curl -X POST \
  http://localhost:8080/algorithms/ \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
	"id": "tf-kaggle-titanic-1",
	"projectId": "kaggle-titanic",
	"backend": {
		"class": "TensorFlowBackend",
		"host": "127.0.0.1",
		"port": 3000,
		"featuresTransformer": {
			"signatureName": "",
			"fields": [
				"passenger_id",
				"p_class",
				"name",
				"sex",
				"age",
				"sib_sp",
				"p_arch",
				"ticket",
				"fare",
				"cabin",
				"embarked"
			]
		},
		"labelsTransformer": {
		    "function": "map",
			"fields": {
				"survived": "survived",
				"did_not_survived": "notSurvived"
			}
		}
	}
}
```

# Start predicting labels
So we should have a working algorithm by now. We can start making predictions. Let's take the first sample of our Kaggle dataset.

```
curl -X POST \
  http://localhost:8080/predictions \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
	"projectId": "kaggle-titanic",
	"algorithmId": "tf-kaggle-titanic-1",
	"features": {
		"class": "CustomFeatures",
		"data": [
			1,
			3,
			"Braund Mr. Owen Harris",
			"male",
			22,
			1,
			0,
			"A/5 21171",
			"7.25",
			"",
			"S"
		]
	}
}
```

If our algorithm is correctly configured, we will get something like below.

```
{
    "id": "5c93c052-be9e-4b1c-bfda-fbd3c0514966",
    "projectId": "kaggle-titanic",
    "algorithmId": "tf-kaggle-titanic-1",
    "features": {
        "data": [
            1,
            3,
            "Braund Mr. Owen Harris",
            "male",
            22,
            1,
            0,
            "A/5 21171",
            "7.25",
            "",
            "S"
        ],
        "class": "CustomFeatures"
    },
    "labels": {
        "labels": [
            {
                "id": "4e1574f9-5376-434d-ada0-b74ed18ca50c",
                "label": "survived",
                "probability": 0,
                "correctExampleUrl": "/examples?predictionId=5c93c052-be9e-4b1c-bfda-fbd3c0514966&labelId=4e1574f9-5376-434d-ada0-b74ed18ca50c&isCorrect=true",
                "incorrectExampleUrl": "/examples?predictionId=5c93c052-be9e-4b1c-bfda-fbd3c0514966&labelId=4e1574f9-5376-434d-ada0-b74ed18ca50c&isIncorrect=true",
                "class": "ClassificationLabel"
            },
            {
                "id": "4f9e4773-a31d-4d49-a63e-360947060363",
                "label": "notSurvived",
                "probability": 1,
                "correctExampleUrl": "/examples?predictionId=5c93c052-be9e-4b1c-bfda-fbd3c0514966&labelId=4f9e4773-a31d-4d49-a63e-360947060363&isCorrect=true",
                "incorrectExampleUrl": "/examples?predictionId=5c93c052-be9e-4b1c-bfda-fbd3c0514966&labelId=4f9e4773-a31d-4d49-a63e-360947060363&isIncorrect=true",
                "class": "ClassificationLabel"
            }
        ]
    },
    "examples": []
}
```

Notice that we predicted the passenger would not survive with a probability of 1 ! Pay attention to the `correctExampleUrl` and `incorrectExampleUrl`. These links are relative to the root of the foundaml server. 

They allow you to label a prediction correct or incorrect. If you can have humans validate your predictions, this is extremely valuable.

You should also be aware that each prediction and example gets published to your favorite streaming platform (Only Amazon Kinesis at the moment). This allows you to evaluate your algorithms in real time.