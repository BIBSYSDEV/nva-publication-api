Feature: Publish a Publication

  Scenario: The User publishes a Publication
    Given the User wants to publish a Publication
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request POST /publication/{identifier}/publish
    Then they receive a response with status code 202
    And they see that the response Content-type is "application/json"
    And they see the response body is a JSON object with the message "Publication is being published. This may take a while."

  Scenario: The User checks status on a published Publication
    Given the User has published the Publication
    But the Publication is still not indexed
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request POST /publication/{identifier}/publish
    Then they receive a response with status code 202
    And they see that the response Content-type is "application/json"
    And they see the response body is a JSON object with the message "Publication is being published. This may take a while."

  Scenario: The User checks status on a published and indexed Publication
    Given the User has published the Publication
    And the Publication is indexed
    When they set the Accept header to "application/json"
    And they set the Authentication header to a Bearer token with their <credentials>
    And they request POST /publication/{identifier}/publish
    Then they receive a response with status code 200
    And they see that the response Content-type is "application/json"
    And they see the response body is a JSON object with the message "Publication is published."