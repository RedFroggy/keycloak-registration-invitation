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

package fr.redfroggy.keycloak.invitation.requiredactions;

import fr.redfroggy.keycloak.invitation.forms.RegistrationInvitationFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WebhookRegistrationSuccess implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {
    private static final Logger logger = Logger.getLogger(WebhookRegistrationSuccess.class);
    public static final String WEBHOOK_REGISTRATION_SUCCESS = "WEBHOOK_REGISTRATION_SUCCESS";

    @Override
    public void evaluateTriggers(RequiredActionContext requiredActionContext) {

    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String error = null;
        LoginFormsProvider form = context.form();
        try {
            registrationSuccessExecute(context);
            context.success();
            return;
        } catch (HttpResponseException e) {
            error = e.getMessage();
            form.setError("WebhookRegistrationSuccessError" + e.getStatusCode(), e.getReasonPhrase());
        } catch (Exception e) {
            error = e.getMessage();
            form.setError("WebhookRegistrationSuccessError", e.getMessage());
        }
        logger.error(error);
        EventBuilder event = context.getEvent().clone().event(EventType.EXECUTE_ACTIONS_ERROR)
                .detail(Details.EMAIL, context.getUser().getEmail())
                .detail(Details.ACTION, WEBHOOK_REGISTRATION_SUCCESS);
        event.error(error);
        Response challenge = form.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
        context.challenge(challenge);
    }


    @Override
    public void processAction(RequiredActionContext context) {
    }


    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return WEBHOOK_REGISTRATION_SUCCESS;
    }

    protected void registrationSuccessExecute(RequiredActionContext context) throws IOException {
        AuthenticatorConfigModel config = context.getRealm().getAuthenticatorConfigByAlias("invitation");
        String url = config.getConfig().get(RegistrationInvitationFactory.REGISTRATION_SUCCESS_URL);

        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost post = new HttpPost(url);

        UserModel user = context.getUser();
        UserRepresentation userRepresentation = ModelToRepresentation.toBriefRepresentation(user);
        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            Map<String, List<String>> attrs = new HashMap(user.getAttributes());
            userRepresentation.setAttributes(attrs);
        }

        post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(userRepresentation), ContentType.APPLICATION_JSON));
        httpClient.execute(post);
        HttpResponse response = httpClient.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 399) {
            throw new HttpResponseException(statusCode, response.getStatusLine().getReasonPhrase());
        }
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession keycloakSession, String displayType) {
        if (displayType == null) return this;
        return null;
    }

    @Override
    public String getDisplayText() {
        return "Update invitation";
    }
}
