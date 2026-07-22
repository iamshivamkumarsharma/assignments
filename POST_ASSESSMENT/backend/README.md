## Environment:
- Java version: 17
- Maven version: 3.*
- Spring Boot version: 3.0.6

## Read-Only Files:
- src/test/*

## Data:
Sample example of JSON data object:
```json
{
  "title": "Lost in Echoes",
  "albumName": "Echoes of the Unknown",
  "releaseDate": "2021-07-15",
  "playCount": 5000
}
```

## Requirements:
The REST service must expose the endpoint music/platform/v1/tracks, enabling the management of music track records as follows: Make sure to add the business logic in TrackServiceImpl.class

POST request to music/platform/v1/tracks: Creates a new music track record. It expects a valid music track data object as its body payload, excluding the id property. The service assigns a unique long id to the added object. The response includes the created record with its unique id, and the response code is 201.

GET request to music/platform/v1/tracks: Responds with a list of all music track records and a response code of 200.

DELETE request to music/platform/v1/tracks/{trackId}: Deletes the record with the specified track id if it exists in the database.

GET request to music/platform/v1/tracks/search: Responds with music track records filtered by title. The response code is 200. It accepts query string parameter title. Records are returned based on matching title.


## Task

Your task is to complete the coding for this **Full Stack Web Application** by implementing all the required functionality in both the **Spring Boot Backend** and the **Angular Frontend**, using **PostgreSQL** as the database.

The application must satisfy **all the provided test cases**. Begin by implementing the backend **POST** endpoint (`POST /music/platform/v1/tracks`), as the remaining functionalities depend on successful creation of records. After completing the backend APIs, develop the Angular frontend to consume these APIs and provide the required user interface.

Ensure the project passes **all the provided test cases** successfully before submission.

## Commands
- run:
```bash
mvn clean package; java -jar target/project_jar-1.0.jar
```
- install:
```bash
mvn clean install
```
- test:
```bash
mvn clean test
```
