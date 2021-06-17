/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.redfroggy.keycloak.invitation.forms;

import fr.redfroggy.keycloak.invitation.requiredactions.WebhookRegistrationSuccess;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RegistrationInvitation implements FormAction {

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String invitationCodeParam = config.getConfig()
                .getOrDefault(RegistrationInvitationFactory.INVITATION_CODE_PARAM, "invitation-code");

        HttpRequest request = context.getHttpRequest();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        String invitationCode = session.getAuthNote("invitationCode");
        if (StringUtils.isEmpty(invitationCode)) {
            invitationCode = request.getUri().getQueryParameters().getFirst(invitationCodeParam);
            if (!Validation.isBlank(invitationCode)) {
                session.setAuthNote("invitationCode", invitationCode);
            }
        }

        if (invitationCode != null) {
            try {
                Map invitation = validateInvitation(context, invitationCode);
                form.setAttribute("invitation", invitation);
                form.setAttribute("invitationCode", invitationCode);
            } catch (HttpResponseException e) {
                form.addError(new FormMessage("invitationCode", "invitationRejected", e.getStatusCode(), e.getReasonPhrase()));
                form.addError(new FormMessage("invitationCode", "invitationRejected" + e.getStatusCode(), e.getReasonPhrase()));
                session.setAuthNote("invitationCodeError", e.getMessage());
            } catch (Exception e) {
                form.addError(new FormMessage(null, "failedInvitationMessage"));
                session.setAuthNote("invitationError", "failedInvitationMessage");
            }
        }
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        String invitationCode = session.getAuthNote("invitationCode");
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String enforcementMode = config.getConfig().get(RegistrationInvitationFactory.INVITATION_ENFORCEMENT_MODE);

        List<FormMessage> errors = new ArrayList<>();
        if (Validation.isBlank(invitationCode) && RegistrationInvitationFactory.ENFORCING.equals(enforcementMode)) {
            errors.add(new FormMessage("invitationCode", "invitationCodeRequired"));
            context.validationError(formData, errors);
            context.error(Errors.INVALID_REGISTRATION);
            return;
        }

        String invitationCodeError = session.getAuthNote("invitationCodeError");
        if (!Validation.isBlank(invitationCodeError)) {
            context.validationError(formData, errors);
            context.error(Errors.INVALID_REGISTRATION);
            session.removeAuthNote("invitationCodeError");
            return;
        }

        String invitationError = session.getAuthNote("invitationError");
        if (!Validation.isBlank(invitationError)) {
            context.validationError(formData, errors);
            context.error(Errors.INVALID_REGISTRATION);
            session.removeAuthNote("invitationError");
            return;
        }

        context.success();
    }

    @Override
    public void success(FormContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config.getConfig().containsKey(RegistrationInvitationFactory.REGISTRATION_SUCCESS_URL)) {
            UserModel user = context.getUser();
            user.addRequiredAction(WebhookRegistrationSuccess.WEBHOOK_REGISTRATION_SUCCESS);
        }
    }


    @Override
    public void close() {

    }

    protected Map validateInvitation(FormContext context, String invitationCode) throws Exception {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String url = config.getConfig().get(RegistrationInvitationFactory.INVITATION_VALIDATION_URL);

        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost post = new HttpPost(url);
        try {
            post.setEntity(new StringEntity(invitationCode));
            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 299) {
                throw new HttpResponseException(
                        statusCode, response.getStatusLine().getReasonPhrase());
            }
            InputStream content = response.getEntity().getContent();
            try {
                return JsonSerialization.readValue(content, Map.class);
            } finally {
                content.close();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
            throw e;
        }
    }
}
