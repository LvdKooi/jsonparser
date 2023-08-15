# JsonParser
_A side project by Laurens van der Kooi ([@LvdKooi](https://github.com/LvdKooi))_ 
## Introduction
A JSON Parser which I'm working on, just for fun. It's ever evolving. It is also my playground to apply a monad which I'm currently developing (the Conditional (please check out the _monad_ package in this project)).

## Technical Description 
The two main classes in this project are **JsonArrayParser** and **JsonObjectParser** in the _parser_ package. Both have one method _parse(String jsonString)_ which is able to deserialize a json-string to a ```List<Object>``` (when calling the JsonArrayParser), or a ```JsonObject``` (when calling the JsonObjectParser).

### JsonObject and JsonNode
The JsonObject is an object containing an array of JsonNodes:

```
public record JsonObject(JsonNode[] jsonNodes) {
}

```

A JsonNode is an object containing a String identifier and an Object containing the content.

```
public record JsonNode(String identifier, Object content) {
}
```

The JsonObjectParser transforms a JSON that looks like this:

```
{
  "firstName": "John",
  "lastName": "Smith",
  "age": 25
}
```

To a JsonObject containing 3 JsonNodes:
* One with the identifier "firstName" containing a String object "John";
* One with the identifier "lastName" containing a String object "Smith";
* One with the identifier "age" containing an integral value of 25.

There is no specific object for a JSON array; a JSON array is being deserialized as a ```List<Object>```. So passing a JSON String that looks like this: 
```
{
 "children": ["Anthony", "Marvin"]
}
```

will result in an JsonObject, containing 1 JsonNode with identifier "children" and content ```List<String>```. 

### Supported data types
* Object
* String
* Boolean
* Integer number
* Floating point number
* Array (of single or mixed types)

### Yet to be implemented
* Null

Please check out the **JsonArrayParserTest** and **JsonObjectParserTest** in the ```jsonparser/src/test``` folder for proven behaviour.