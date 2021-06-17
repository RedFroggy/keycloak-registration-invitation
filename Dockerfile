FROM jboss/keycloak:11.0.3

ENV DB_VENDOR H2

ADD target/keycloak-registration-invitation-*-SNAPSHOT.jar /opt/jboss/keycloak/standalone/deployments/


