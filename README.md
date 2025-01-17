# nva-publication-api

This repository contains the core services of NVA (Nasjonalt vitenarkiv) related to creating and administering publications (registrations, results).

This is an AWS serverless application.

## Service diagram

## Link to descriptions of modules


## Deployment dependencies

This application is deployed in AWS via CodePipeline. Build/deploy pipelines are defined in [nva-infrastructure](https://github.com/BIBSYSDEV/NVA-infrastructure). Some services depend on services from [nva-common-resources](https://github.com/BIBSYSDEV/nva-common-resources) and [nva-identity-service](https://github.com/BIBSYSDEV/nva-identity-service).

## Development

1. Install correct version of Java (see [buildspec.yaml](buildspec.yaml) for current target)
2. Clone the repository
3. Run gradle build

## Contributing code

The build is configured to maintain code standards. Contribute via PR with description and context for change.
