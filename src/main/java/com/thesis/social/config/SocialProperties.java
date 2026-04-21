package com.thesis.social.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "social")
public class SocialProperties {

    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Websocket websocket = new Websocket();
    private final Broker broker = new Broker();
    private final Event event = new Event();

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
    }

    public Websocket getWebsocket() {
        return websocket;
    }

    public Broker getBroker() {
        return broker;
    }

    public Event getEvent() {
        return event;
    }

    public static class Cors {
        private String allowedOrigins;

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Security {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            private String publicKey;
            private String signingKid;
            private String issuer;
            private String audience;

            public String getPublicKey() {
                return publicKey;
            }

            public void setPublicKey(String publicKey) {
                this.publicKey = publicKey;
            }

            public String getSigningKid() {
                return signingKid;
            }

            public void setSigningKid(String signingKid) {
                this.signingKid = signingKid;
            }

            public String getIssuer() {
                return issuer;
            }

            public void setIssuer(String issuer) {
                this.issuer = issuer;
            }

            public String getAudience() {
                return audience;
            }

            public void setAudience(String audience) {
                this.audience = audience;
            }
        }
    }

    public static class Websocket {
        private String endpoint;
        private String appDestinationPrefix;
        private String userDestinationPrefix;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAppDestinationPrefix() {
            return appDestinationPrefix;
        }

        public void setAppDestinationPrefix(String appDestinationPrefix) {
            this.appDestinationPrefix = appDestinationPrefix;
        }

        public String getUserDestinationPrefix() {
            return userDestinationPrefix;
        }

        public void setUserDestinationPrefix(String userDestinationPrefix) {
            this.userDestinationPrefix = userDestinationPrefix;
        }
    }

    public static class Broker {
        private String relayHost;
        private Integer relayPort;
        private String relayClientLogin;
        private String relayClientPasscode;
        private String relaySystemLogin;
        private String relaySystemPasscode;

        public String getRelayHost() {
            return relayHost;
        }

        public void setRelayHost(String relayHost) {
            this.relayHost = relayHost;
        }

        public Integer getRelayPort() {
            return relayPort;
        }

        public void setRelayPort(Integer relayPort) {
            this.relayPort = relayPort;
        }

        public String getRelayClientLogin() {
            return relayClientLogin;
        }

        public void setRelayClientLogin(String relayClientLogin) {
            this.relayClientLogin = relayClientLogin;
        }

        public String getRelayClientPasscode() {
            return relayClientPasscode;
        }

        public void setRelayClientPasscode(String relayClientPasscode) {
            this.relayClientPasscode = relayClientPasscode;
        }

        public String getRelaySystemLogin() {
            return relaySystemLogin;
        }

        public void setRelaySystemLogin(String relaySystemLogin) {
            this.relaySystemLogin = relaySystemLogin;
        }

        public String getRelaySystemPasscode() {
            return relaySystemPasscode;
        }

        public void setRelaySystemPasscode(String relaySystemPasscode) {
            this.relaySystemPasscode = relaySystemPasscode;
        }
    }

    public static class Event {
        private String exchange;
        private String routingPrefix;

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingPrefix() {
            return routingPrefix;
        }

        public void setRoutingPrefix(String routingPrefix) {
            this.routingPrefix = routingPrefix;
        }
    }
}
