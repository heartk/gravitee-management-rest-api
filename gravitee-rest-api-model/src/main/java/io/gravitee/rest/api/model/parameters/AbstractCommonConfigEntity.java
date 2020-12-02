/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.parameters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.rest.api.model.annotations.ParameterKey;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(value = {"baseURL", "orgBaseURL", "envBaseURL"})
public abstract class AbstractCommonConfigEntity {

    public static final String METADATA_READONLY = "readonly";
    private Analytics analytics;
    private Email email;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private MultiValueMap<String, String> metadata;

    public AbstractCommonConfigEntity() {
        analytics = new Analytics();
        email = new Email();
        metadata = new LinkedMultiValueMap<>();
    }
    // Getters & setters
    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public MultiValueMap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(MultiValueMap<String, String> metadata) {
        this.metadata = metadata;
    }

    // Classes
    public static class Analytics {
        @ParameterKey(Key.ANALYTICS_CLIENT_TIMEOUT)
        private Long clientTimeout;

        public Long getClientTimeout() {
            return clientTimeout;
        }
        public void setClientTimeout(Long clientTimeout) {
            this.clientTimeout = clientTimeout;
        }
    }

    public static class Email {
        @ParameterKey(Key.EMAIL_ENABLED)
        private Boolean enabled;

        @ParameterKey(Key.EMAIL_HOST)
        private String host;

        @ParameterKey(Key.EMAIL_PORT)
        private Integer port;

        @ParameterKey(Key.EMAIL_USERNAME)
        private String username;

        @ParameterKey(Key.EMAIL_PASSWORD)
        private String password;

        @ParameterKey(Key.EMAIL_PROTOCOL)
        private String protocol;

        @ParameterKey(Key.EMAIL_SUBJECT)
        private String subject;

        @ParameterKey(Key.EMAIL_FROM)
        private String from;

        private EmailProperties properties;

        public Email() {
            properties = new EmailProperties();
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public EmailProperties getProperties() {
            return properties;
        }

        public void setProperties(EmailProperties properties) {
            this.properties = properties;
        }

        public static class EmailProperties {
            @ParameterKey(Key.EMAIL_PROPERTIES_AUTH_ENABLED)
            private Boolean auth;

            @ParameterKey(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE)
            private Boolean startTlsEnable;

            @ParameterKey(Key.EMAIL_PROPERTIES_SSL_TRUST)
            private String sslTrust;

            public Boolean getAuth() {
                return auth;
            }

            public void setAuth(Boolean auth) {
                this.auth = auth;
            }

            public Boolean getStartTlsEnable() {
                return startTlsEnable;
            }

            public void setStartTlsEnable(Boolean startTlsEnable) {
                this.startTlsEnable = startTlsEnable;
            }

            public String getSslTrust() {
                return sslTrust;
            }

            public void setSslTrust(String sslTrust) {
                this.sslTrust = sslTrust;
            }
        }
    }

    // Common classes
    public static class Enabled {
        private boolean enabled;

        Enabled() {
        }

        public Enabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
