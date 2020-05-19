Feature: Update an existing Publication

  Scenario: An Anonymous User attempts to update a Publication
    Given a User with role Anonymous
    When they set the Accept header to "application/json"
    And they set the request body to UpdatePublicationRequest
    And they request PUT /publication/{identifier}
    Then they receive a response with status code 401
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Unauthorized"
    And they see the response body has a field "status" with the value "401"
    And they see the response body has a field "detail" with a description of the problem

  Scenario: A User attempts to update a Publication with invalid metadata
    Given a User with role Creator
    And they are the Owner
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}
    Then they receive a response with status code 400
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad Request"
    And they see the response body has a field "status" with the value "400"
    And they see the response body has a field "detail" with a description of the problem

  Scenario: A User attempts to update a missing Publication
    Given a User with role Creator
    And they are the Owner
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}
    Then they receive a response with status code 404
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Not found"
    And they see the response body has a field "status" with the value "404"
    And they see the response body has a field "detail" with a description of the problem

  Scenario: A User updates a Publication
    Given a User with role Creator
    And they are the Owner
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they set the request body to UpdatePublicationRequest
    And they request PUT /publication/{identifier}
    Then they receive a response with status code 200
    And they see that the response Content-Type header is "application/json"
    And they see that the response body is a PublicationResponse