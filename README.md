# keycloak-registration-invitation

Register user with invitation

This [Keycloak](https://www.keycloak.org) plugin adds a form action and a required action to register user with
invitation.

[![Build Status](https://github.com/RedFroggy/keycloak-registration-invitation/actions/workflows/tag.yml/badge.svg)](https://github.com/RedFroggy/keycloak-verify-email-by-code)

## Features

* Validate invitation from external url
* Show invitation details in register form
* Call external url when registration successfull (WebhookRegistrationSuccess required action)

## Compatibility

The version 11.0 of this plugin is compatible with Keycloak `11.0.3` and higher.

## Installation

The plugin installation is simple and can be done without a Keycloak server restart.

* Download the latest release from
  the [releases page](https://github.com/RedFroggy/keycloak-registration-invitation/releases)
* Copy the JAR file into the `standalone/deployments` directory in your Keycloak server's root
* Restart Keycloak (optional, hot deployment should work)

You can also clone the Github Repository and install the plugin locally with the following command:

```
$ mvn clean install wildfly:deploy
```

## How to use it

### Requirements

Form action 'RegistrationInvitation' and 'WebhookRegistrationSuccess' required action are deploy in keycloak. Got to
{keycloak url}/auth/admin/master/console/#/server-info/providers.

![server-info-providers-required-action](/assets/server-info-providers-required-action.png)
![server-info-providers-form-action](/assets/server-info-providers-form-action.png)

### Configuration

#### WebhookRegistrationSuccess required action

Once the installation is complete, the `Webhook Registration Success` required action appears in "
authentication/required-actions" on your realm. Register and enable `Webhook Registration Success`.
![required-actions-conf](/assets/register-action.png)

#### RegistrationInvitation form

Create your own 'registration flow' in /authentication/flows/registration on your realm.

1. Copy registration to 'Registration With Invitation'
   ![copy-registration-flow](/assets/copy-registration-flow.png)
1. Add Invitation Validation execution to 'Registration With Invitation' Registration Form
   ![add-execution](/assets/add-execution.png)
1. Add config to 'Invitation Validation'
   ![add-invitationForm-config](/assets/add-invitationForm-config.png)
1. Save config with alias 'invitation', the alias must be 'invitation'
   ![save-invitation-config](/assets/save-invitation-config.png)

#### Templates

You can override individual templates in your own theme. To create a custom register form for the mytheme theme copy
[register.ftl](/src/main/resources/theme/redfroggy/login/register.ftl) (in redfroggy theme) to themes/mytheme/login/.

## How to test it

1. clone this repo git@github.com:RedFroggy/keycloak-registration-invitation.gi

``` bash
    git clone git@github.com:RedFroggy/keycloak-verify-email-by-code.git
``` 

2. package jar

``` bash

    mvn package
``` 

3. run docker-compose

``` bash
    docker-compose up -d --build --force-recreate
``` 

4.
test [registration](http://localhost:9080/auth/realms/redfroggy/protocol/openid-connect/registrations?client_id=web_app&response_type=code&scope=openid%20email&redirect_uri=http://localhost:9080/auth/realms/redfroggy/account&kc_locale=fr&invitation-code=my-invitation-code)

## Q&A

[See Q&A](FAQ.md)

## How to contribute

[See here](CONTRIBUTING.en.md)