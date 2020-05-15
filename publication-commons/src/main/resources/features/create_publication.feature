Feature: Create a new Publication

  Scenario: An Anonymous User wants to create a new Publication
    Given an Anonymous User wants to create a new Publication
    When they set the Accept header to "application/json"
    And they request POST /publication/
    Then they receive a response with status code 401
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Unauthorized"
    And they see the response body has a field "status" with the value "401"

  Scenario: a User wants to create a new Publication
    Given a User wants to create a new Publication
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request POST /publication/
    Then they receive a response with status code 201
    And they see that the response Location header has a value
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a PublicationResponse
    And they see that the response body has a property identifier

  Scenario: A User wants to create a new Publication with metadata
    Given a User wants to create a new Publication with metadata
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they set the request body to CreatePublicationRequest
    And the request body has a property entityDescription
    And the request body has a property project
    And the request body has a property @context
    And they request POST /publication/
    Then they receive a response with status code 201
    And they see that the response Location header has a value
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a PublicationResponse
    And they see that the response body has a property identifier

  Scenario: A User wants to create a new Publication with file
    Given a User wants to create a new Publication with file
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they set the request body to CreatePublicationRequest
    And the request body has a property fileSet
    And the request body has a property @context
    And they request POST /publication/
    Then they receive a response with status code 201
    And they see that the response Location header has a value
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a PublicationResponse
    And they see that the response body has a property identifier