# FDK Maskinporten Exchange

This application provides an API for exchanging tokens with Maskinporten. It handles authentication and token management for accessing Maskinporten-protected resources.

For a broader understanding of the system's context, refer to the architecture documentation wiki. For more specific context on this application, see the Maskinporten integration section.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Ensure you have the following installed:

- Java 25
- Maven
- Docker (optional)

### Running locally

1. Clone the repository

```bash
git clone https://github.com/Informasjonsforvaltning/fdk-maskinporten-exchange.git
cd fdk-maskinporten-exchange
```

2. Configure environment variables

Set the following environment variables or use `application-local.yml`:

- `MASKINPORTEN_CLIENT_ID`
- `MASKINPORTEN_KEY_ID`
- `MASKINPORTEN_PRIVATE_KEY`
- `MASKINPORTEN_ISSUER` (optional, defaults to https://maskinporten.no)
- `MASKINPORTEN_TOKEN_ENDPOINT` (optional, defaults to https://maskinporten.no/token)
- `MASKINPORTEN_JWKS_ENDPOINT` (optional, defaults to https://maskinporten.no/jwk)
- `MASKINPORTEN_SCOPE` (optional, defaults to altinn:accessmanagement/authorizedparties.resourceowner)

3. Start the application (either through your IDE using the test profile, or via CLI):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Or use the local profile with `application-local.yml`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## API Documentation (OpenAPI)

Once the application is running locally, the API documentation can be accessed at http://localhost:8080/swagger-ui.html

## Running tests

```bash
mvn verify
```
