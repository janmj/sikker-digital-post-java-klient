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
package no.difi.sdp.client.domain.kvittering;

import no.digipost.api.representations.EbmsApplikasjonsKvittering;
import no.digipost.api.representations.SimpleStandardBusinessDocument;
import org.joda.time.DateTime;

import java.util.Date;

public abstract class ForretningsKvittering {

    public final EbmsApplikasjonsKvittering applikasjonsKvittering;
    private final DateTime tidspunkt;

    protected ForretningsKvittering(EbmsApplikasjonsKvittering applikasjonsKvittering) {
        this.applikasjonsKvittering = applikasjonsKvittering;
        SimpleStandardBusinessDocument sbd = applikasjonsKvittering.getStandardBusinessDocument();
		if (sbd.erFeil()) {
        	this.tidspunkt = sbd.getFeil().getTidspunkt();
        } else if (sbd.erKvittering()) {
        	this.tidspunkt = sbd.getKvittering().kvittering.getTidspunkt();
        } else {
        	throw new IllegalStateException("Unable to handle StandardBusinessDocument of type " +
        			sbd.getUnderlyingDoc().getClass() + ", conversationId=" + sbd.getConversationId());
        }
    }

    public String getKonversasjonsId() {
        return applikasjonsKvittering.getStandardBusinessDocument().getConversationId();
    }

    public final Date getTidspunkt() {
    	return tidspunkt.toDate();
    }

    public String getMessageId() {
        return applikasjonsKvittering.messageId;
    }

    public String getRefToMessageId() {
        return applikasjonsKvittering.refToMessageId;
    }

    /**
     * Gir hvilken subtype av ForretningsKvittering og konversasjonsId som String.
     * Subklasser kan override dette.
     */
    @Override
    public String toString() {
    	return this.getClass().getSimpleName() + "{" +
                "konversasjonsId=" + getKonversasjonsId() +
                "}";
    }

}
