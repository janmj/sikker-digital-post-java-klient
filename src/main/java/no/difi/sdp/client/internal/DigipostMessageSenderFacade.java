/**
 * Copyright (C) Posten Norge AS
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
package no.difi.sdp.client.internal;

import no.difi.sdp.client.ExceptionMapper;
import no.difi.sdp.client.KlientKonfigurasjon;
import no.difi.sdp.client.domain.TekniskAvsender;
import no.difi.sdp.client.domain.exceptions.KonfigurasjonException;
import no.difi.sdp.client.domain.exceptions.SendException;
import no.difi.sdp.client.domain.exceptions.XmlValideringException;
import no.digipost.api.MessageSender;
import no.digipost.api.interceptors.KeyStoreInfo;
import no.digipost.api.interceptors.TransactionLogClientInterceptor;
import no.digipost.api.interceptors.WsSecurityInterceptor;
import no.digipost.api.representations.EbmsAktoer;
import no.digipost.api.representations.EbmsApplikasjonsKvittering;
import no.digipost.api.representations.EbmsForsendelse;
import no.digipost.api.representations.EbmsPullRequest;
import no.digipost.api.xml.Schemas;
import org.apache.http.HttpRequestInterceptor;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.client.support.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.context.MessageContext;
import org.xml.sax.SAXParseException;

import java.util.Arrays;

import static no.difi.sdp.client.domain.exceptions.SendException.AntattSkyldig.*;

public class DigipostMessageSenderFacade {

    private final MessageSender messageSender;
    private ExceptionMapper exceptionMapper = new ExceptionMapper();

    public DigipostMessageSenderFacade(final TekniskAvsender avsender, final KlientKonfigurasjon konfigurasjon) {
        KeyStoreInfo keyStoreInfo = avsender.noekkelpar.getKeyStoreInfo();
        WsSecurityInterceptor wsSecurityInterceptor = new WsSecurityInterceptor(keyStoreInfo, new UserFriendlyWsSecurityExceptionMapper());
        wsSecurityInterceptor.afterPropertiesSet();

        MessageSender.Builder messageSenderBuilder = MessageSender.create(konfigurasjon.getMeldingsformidlerRoot().toString(),
                keyStoreInfo,
                wsSecurityInterceptor,
                EbmsAktoer.avsender(avsender.organisasjonsnummer),
                EbmsAktoer.meldingsformidler(konfigurasjon.getMeldingsformidlerOrganisasjon()))
                .withConnectTimeout((int) konfigurasjon.getConnectTimeoutInMillis())
                .withSocketTimeout((int) konfigurasjon.getSocketTimeoutInMillis())
                .withConnectionRequestTimeout((int) konfigurasjon.getConnectionRequestTimeoutInMillis())
                .withDefaultMaxPerRoute(konfigurasjon.getMaxConnectionPoolSize()) // Vi vil i praksis bare kjøre én route med denne klienten.
                .withMaxTotal(konfigurasjon.getMaxConnectionPoolSize());

        if (konfigurasjon.useProxy()) {
            messageSenderBuilder.withHttpProxy(konfigurasjon.getProxyHost(), konfigurasjon.getProxyPort(), konfigurasjon.getProxyScheme());
        }

        // Legg til http request interceptors fra konfigurasjon pluss vår egen.
        HttpRequestInterceptor[] httpRequestInterceptors = Arrays.copyOf(konfigurasjon.getHttpRequestInterceptors(), konfigurasjon.getHttpRequestInterceptors().length + 1);
        httpRequestInterceptors[httpRequestInterceptors.length - 1] = new AddClientVersionInterceptor();
        messageSenderBuilder.withHttpRequestInterceptors(httpRequestInterceptors);

        messageSenderBuilder.withHttpResponseInterceptors(konfigurasjon.getHttpResponseInterceptors());

        messageSenderBuilder.withMeldingInterceptorBefore(TransactionLogClientInterceptor.class, payloadValidatingInterceptor());

        for (ClientInterceptor clientInterceptor : konfigurasjon.getSoapInterceptors()) {
            // TransactionLogClientInterceptoren bør alltid ligge ytterst for å sikre riktig transaksjonslogging (i tilfelle en custom interceptor modifiserer requestet)
            messageSenderBuilder.withMeldingInterceptorBefore(TransactionLogClientInterceptor.class, clientInterceptor);
        }

        messageSender = messageSenderBuilder.build();
    }

    public void send(final EbmsForsendelse ebmsForsendelse) {
        performRequest(new VoidRequest() {
            @Override
            public void exec() {
                messageSender.send(ebmsForsendelse);
            }
        });
    }

    public EbmsApplikasjonsKvittering hentKvittering(final EbmsPullRequest ebmsPullRequest) {
        return performRequest(new Request<EbmsApplikasjonsKvittering>() {
            @Override
            public EbmsApplikasjonsKvittering exec() {
                return messageSender.hentKvittering(ebmsPullRequest);
            }
        });
    }

    public EbmsApplikasjonsKvittering hentKvittering(final EbmsPullRequest ebmsPullRequest, final EbmsApplikasjonsKvittering applikasjonsKvittering) {
        return performRequest(new Request<EbmsApplikasjonsKvittering>() {
            @Override
            public EbmsApplikasjonsKvittering exec() {
                return messageSender.hentKvittering(ebmsPullRequest, applikasjonsKvittering);
            }
        });
    }

    public void bekreft(final EbmsApplikasjonsKvittering kvittering) {
        performRequest(new VoidRequest() {
            @Override
            public void exec() {
                messageSender.bekreft(kvittering);
            }
        });

    }

    private void performRequest(final VoidRequest request) {
        this.performRequest(new Request<Object>() {
            @Override
            public Object exec() {
                request.exec();
                return null;
            }
        });
    }

    private <T> T performRequest(final Request<T> request) throws SendException {
        try {
            return request.exec();
        }
        catch (RuntimeException e) {
            RuntimeException mappedException = exceptionMapper.mapException(e);
            if (mappedException != null) {
                throw mappedException;
            }

            throw new SendException("An unhandled exception occured while performing request", UKJENT, e);
        }
    }

    private interface VoidRequest {
        void exec();
    }

    private interface Request<T> {
        T exec();
    }

    public void setExceptionMapper(final ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

    protected ClientInterceptor payloadValidatingInterceptor() {
        try {
            PayloadValidatingInterceptor payloadValidatingInterceptor = new PayloadValidatingInterceptor() {
                @Override
                protected boolean handleRequestValidationErrors(final MessageContext messageContext, final SAXParseException[] errors) {
                    if (messageContext.hasResponse()) {
                        // Feil i responsen, sannsynligvis serveren sin skyld
                        throw new XmlValideringException("XML validation errors in response from server", errors, SERVER);
                    }
                    else {
                        throw new XmlValideringException("XML validation errors in request. Maybe some fields are not being set or are set with null values?", errors, KLIENT);
                    }

                }
            };
            payloadValidatingInterceptor.setSchemas(Schemas.allSchemaResources());
            payloadValidatingInterceptor.setValidateRequest(true);
            // TODO: Responsevalidering skal skrus på når vi er sikre på at MF og postkassene leverer skikkelige responser
            payloadValidatingInterceptor.setValidateResponse(false);
            payloadValidatingInterceptor.afterPropertiesSet();
            return payloadValidatingInterceptor;
        } catch (Exception e) {
            throw new KonfigurasjonException("Unable to initialize payload validating interecptor", e);
        }
    }

}
