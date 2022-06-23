Feature: API tests for Cristin Project retrieve and search

  Background:
#    * def domainName = java.lang.System.getenv('DOMAIN_NAME')
#    * def basePath = java.lang.System.getenv('BASE_PATH')
    * def domainName = 'example.org'
    * def basePath = 'publication'
    * def PUBLICATION_API_ROOT = 'https://' + domainName +'/' + basePath
#    * def customerOrganization = java.lang.System.getenv('CUSTOMER_TEST_ORGANIZATION')
#    * def randomOrganization = java.lang.System.getenv('RANDOM_TEST_ORGANIZATION')
#    * def creatorUsername = java.lang.System.getenv('CREATOR_ID')
#    * def creatorPassword = java.lang.System.getenv('CREATOR_PASSWORD')
#    * def curatorUsername = java.lang.System.getenv('CURATOR_ID')
#    * def curatorPassword = java.lang.System.getenv('CURATOR_PASSWORD')
#    * def cognitoClientAppId = java.lang.System.getenv('COGNITO_CLIENT_APP_ID')
#    * def cognitoUserpoolId = java.lang.System.getenv('COGNITO_USER_POOL_ID')
#    * def tokenGenerator = Java.type('no.unit.nva.cognito.CognitoUtil')
#    * def token = tokenGenerator.loginUser(username, password, cognitoClientAppId)
    * def publicationIdentifier = 'somePublicationIdentifier'
    Given url PUBLICATION_API_ROOT

  Scenario: Create PublishingRequest returns status 401 Unauthorized when user not logged in
    Given path '/' + publicationIdentifier +'/publishingrequest'
    When method POST
    Then status 401

  Scenario: Create PublishingRequest returns status 404 Not Found when input has wrong id
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method POST
    Then status 404

  Scenario: Create PublishingRequest returns status 201 Created when input is valid and persisted
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method POST
    Then status 201

  Scenario: Approve PublishingRequest returns status 403 Forbidden when user don't have right to approve publish request
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method POST
    Then status 403

  Scenario: Approve PublishingRequest returns status 200 Ok when user have right to approve publish request
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method PUT
    Then status 200

  Scenario: Approve PublishingRequest returns status 404 Not Found when user have right to approve publish request
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method PUT
    Then status 403

  Scenario: list PublishingRequest returns status 200 and list of publish requests for curator
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method GET
    Then status 200

  Scenario: list PublishingRequest returns status 200 and list of own publish requests for creator
    Given path '/' + publicationIdentifier + '/publishingrequest'
    When method GET
    Then status 200
