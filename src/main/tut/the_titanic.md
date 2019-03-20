---
layout: page
position: 3
section: home
title:  "Example: The Titanic"
---

# The Titanic
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

The algorithm should predict whether or not the person survived. This is a classification problem. We can create the project with the POST request below

```
{
	"id": "kaggle-titanic",
    "name": "Kaggle Titanic",
    "configuration": {
        "problem": {
            "class": "Classification"
        },
        "features": {
        	"featuresClasses": [
        		{
        			"key": "passengerId",
        			"featureClass": "IntFeature",
        			"description": "The unique identifier of the passenger"
        		},
        		{
        			"key": "pClass",
        			"featureClass": "IntFeature",
        			"description": "Class of travel"
        		},
        		{
        			"key": "name",
        			"featureClass": "StringFeature",
        			"description": "Name of passenger"
        		},
        		{
        			"key": "sex",
        			"featureClass": "StringFeature",
        			"description": "Gender"
        		},
        		{
        			"key": "age",
        			"featureClass": "IntFeature",
        			"description": "Age"
        		},
        		{
        			"key": "sibSp",
        			"featureClass": "IntFeature",
        			"description": "Number of Sibling/Spouse aboard"
        		},
        		{
        			"key": "pArch",
        			"featureClass": "IntFeature",
        			"description": "Number of Parent/Child aboard"
        		},
        		{
        			"key": "ticket",
        			"featureClass": "StringFeature",
        			"description": "The ticket identifier"
        		},
        		{
        			"key": "fare",
        			"featureClass": "StringFeature",
        			"description": "Which fare"
        		},
        		{
        			"key": "cabin",
        		     "featureClass": "StringFeature",
        			"description": "Which cabin"
        		},
        			{
        			"key": "embarked",
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
}
```

# Our first algorithm, a simple heuristic
