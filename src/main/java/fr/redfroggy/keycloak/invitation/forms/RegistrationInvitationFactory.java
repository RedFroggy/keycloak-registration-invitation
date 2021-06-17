package fr.redfroggy.keycloak.invitation.forms;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class RegistrationInvitationFactory implements FormActionFactory {
    public static final String PROVIDER_ID = "registration-invitation";
    public static final String INVITATION_VALIDATION_URL = "invitation-validation-url";
    public static final String REGISTRATION_SUCCESS_URL = "registration-success-url";
    public static final String INVITATION_CODE_PARAM = "invitation-code-param";
    public static final String INVITATION_ENFORCEMENT_MODE = "invitation-enforcement-mode";
    public static final String ENFORCING = "enforcing";
    public static final String PERMISSIVE = "permissive";

    static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES;

    public RegistrationInvitationFactory() {
        this.REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    /**
     * ******** implements FormActionFactory *********
     **/

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Invitation validation";
    }

    @Override
    public String getHelpText() {
        return "Validates invitation of the user in validation phase.  In success phase, this will add required action to update invitation.";
    }

    @Override
    public String getReferenceCategory() {
        return "invitation";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ArrayList props = new ArrayList();
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(INVITATION_VALIDATION_URL);
        property.setLabel("Invitation validation url");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("External url called to validate invitation");
        props.add(property);

        property = new ProviderConfigProperty();
        property.setName(INVITATION_CODE_PARAM);
        property.setLabel("Invitation request param name");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Clients can verify invitation by using the 'invitation-code' parameter in the URL they use to register new user");
        property.setDefaultValue("invitation-code");
        props.add(property);

        property = new ProviderConfigProperty();
        property.setName(REGISTRATION_SUCCESS_URL);
        property.setLabel("Registration success url");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("POST user representation to this url after successful registration");
        props.add(property);

        property = new ProviderConfigProperty();
        property.setName(INVITATION_ENFORCEMENT_MODE);
        property.setLabel("Invitation enforcement mode");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setHelpText("The invitation enforcement mode dictates how invitations are enforced when registering a new user." +
                " 'Enforcing' means registering are denied by default  when there is no invitation associated during registering." +
                " 'Permissive' means registering are allowed even when there is no invitation associated with a given user.");
        property.setOptions(asList(ENFORCING, PERMISSIVE));
        props.add(property);

        return props;
    }

    @Override
    public FormAction create(KeycloakSession keycloakSession) {
        return new RegistrationInvitation();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }
}