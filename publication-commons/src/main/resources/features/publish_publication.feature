Feature: Publish a Publication

  Scenario: A User without required permissions tries to publish a Publication
    Given a User wants to publish a Publication
    But they are missing the required permissions
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}/publish
    Then they receive a response with status code 401
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Unauthorized"
    And they see the response body has a field "status" with the value "401"

  Scenario Outline: The <User> publishes a Publication
    Given the <User> wants to publish a Publication
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}/publish
    Then they receive a response with status code 202
    And they see that the response Content-type is "application/json"
    And they see that the response Location header is the URI to the Publication
    And they see the response body has a field "message" with the value "Publication is being published. This may take a while."

    Examples:
      | User    |
      | Owner   |
      | Curator |
      | Editor  |

  Scenario: The Owner publishes a Publication with a malformed identifier
    Given the Owner wants to publish a Publication
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{malformed identifier}/publish
    Then they receive a response with status code 400
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Bad Request"
    And they see the response body has a field "status" with the value "400"
    And they see the response body has a field "detail" with the value "The request identifier is invalid: {malformed identifier}"

  Scenario: The Owner publishes an already published Publication
    Given the Owner has published the Publication
    But the Publication is still not indexed
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}/publish
    Then they receive a response with status code 202
    And they see that the response Content-type is "application/json"
    And they see that the response Location header is the URI to the Publication
    And they see the response body has a field "message" with the value "Publication is being published. This may take a while."

  Scenario: The Owner publishes an already published and indexed Publication
    Given the Owner has published the Publication
    And the Publication is indexed
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}/publish
    Then they receive a response with status code 200
    And they see that the response Content-type is "application/json"
    And they see that the response Location header is the URI to the Publication
    And they see the response body has a field "message" with the value "Publication is published."

  Scenario: The Owner checks that the Publication is published
    Given the Owner has published the Publication
    And the Publication is indexed
    When they set the Accept header to "application/json"
    And they request GET /publication/{identifier}
    Then they receive a response with status code 200
    And they see that the response Content-type is "application/json"
    And they see that the response body is a Publication JSON object
    And they see that the response body has a field "status" with the value "PUBLISHED"
    And they see that the response body has a field "indexedDate" with a value "{datetime}"

  Scenario: The Owner publishes an invalid Publication
    Given the Owner wants to publish an invalid Publication
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their credentials
    And they request PUT /publication/{identifier}/publish
    Then they receive a response with status code 500
    And they see that the response Content-Type header is "application/problem+json"
    And they see that the response body is a problem.json object
    And they see the response body has a field "title" with the value "Internal Server Error"
    And they see the response body has a field "status" with the value "500"
    And they see the response body has a field "detail" with the value "Publication is missing data required for publishing."