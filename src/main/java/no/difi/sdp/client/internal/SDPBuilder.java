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

import no.difi.begrep.sdp.schema_v10.*;
import no.difi.sdp.client.domain.Behandlingsansvarlig;
import no.difi.sdp.client.domain.Dokument;
import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.client.domain.digital_post.DigitalPost;
import no.difi.sdp.client.domain.digital_post.EpostVarsel;
import no.difi.sdp.client.domain.digital_post.SmsVarsel;
import no.difi.sdp.client.domain.fysisk_post.FysiskPost;
import no.difi.sdp.client.domain.fysisk_post.KonvoluttAdresse;
import no.difi.sdp.client.domain.fysisk_post.KonvoluttAdresse.Type;
import org.joda.time.DateTime;
import org.w3.xmldsig.Reference;
import org.w3.xmldsig.Signature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static no.difi.sdp.client.domain.fysisk_post.KonvoluttAdresse.Type.NORSK;
import static no.difi.sdp.client.domain.fysisk_post.KonvoluttAdresse.Type.UTENLANDSK;

@SuppressWarnings("ConstantConditions")
public class SDPBuilder {

    /**
     * ISO6523-identifikasjon av Brønnøysundregisterets organisasjonsnummer som identifikator.
     */
    private static final String ORGNR_IDENTIFIER = "9908:";

    public SDPManifest createManifest(final Forsendelse forsendelse) {
        SDPMottaker sdpMottaker = sdpMottaker(forsendelse.getDigitalPost());
        SDPAvsender sdpAvsender = sdpAvsender(forsendelse.getBehandlingsansvarlig());
        String spraakkode = forsendelse.getSpraakkode();
        SDPDokument sdpHovedDokument = sdpDokument(forsendelse.getDokumentpakke().getHoveddokument(), spraakkode);

        List<SDPDokument> sdpVedlegg = new ArrayList<SDPDokument>();
        for (Dokument dokument : forsendelse.getDokumentpakke().getVedlegg()) {
            sdpVedlegg.add(sdpDokument(dokument, spraakkode));
        }

        return new SDPManifest(sdpMottaker, sdpAvsender, sdpHovedDokument, sdpVedlegg);
    }

    public SDPDigitalPost buildDigitalPost(final Forsendelse forsendelse) {
        SDPAvsender sdpAvsender = sdpAvsender(forsendelse.getBehandlingsansvarlig());
        SDPMottaker sdpMottaker = sdpMottaker(forsendelse.getDigitalPost());

        SDPDigitalPostInfo sdpDigitalPostInfo = sdpDigitalPostinfo(forsendelse);
        SDPFysiskPostInfo fysiskPostInfo = sdpFysiskPostInfo(forsendelse.getFysiskPost());

        // Signatur og fingeravtrykk legges på automatisk av klienten hvis de ikke er satt
        Signature signature = null;
        Reference dokumentpakkefingeravtrykk = null;

        return new SDPDigitalPost(signature, sdpAvsender, sdpMottaker, sdpDigitalPostInfo, fysiskPostInfo, dokumentpakkefingeravtrykk);
    }

	private SDPDokument sdpDokument(final Dokument dokument, final String spraakkode) {
        SDPTittel sdpTittel = new SDPTittel(dokument.getTittel(), spraakkode);
        return new SDPDokument(sdpTittel, dokument.getFilnavn(), dokument.getMimeType());
    }

    private SDPMottaker sdpMottaker(final DigitalPost digitalPost) {
    	if (digitalPost == null) {
    		return null;
    	}
        SDPPerson sdpPerson = new SDPPerson(digitalPost.getMottaker().getPersonidentifikator(), digitalPost.getMottaker().getPostkasseadresse());
        return new SDPMottaker(sdpPerson);
    }

    private SDPAvsender sdpAvsender(final Behandlingsansvarlig avsender) {
        String fakturaReferanse = avsender.getFakturaReferanse();
        String identifikator = avsender.getAvsenderIdentifikator();
        SDPOrganisasjon organisasjon = sdpOrganisasjon(avsender);

        return new SDPAvsender(organisasjon, identifikator, fakturaReferanse);
    }

    private SDPOrganisasjon sdpOrganisasjon(final Behandlingsansvarlig avsender) {
        return new SDPOrganisasjon(ORGNR_IDENTIFIER + avsender.getOrganisasjonsnummer(), SDPIso6523Authority.ISO_6523_ACTORID_UPIS);
    }

    private SDPDigitalPostInfo sdpDigitalPostinfo(final Forsendelse forsendelse) {
        DigitalPost digitalPost = forsendelse.getDigitalPost();
        if (digitalPost == null) {
        	return null;
        }

        DateTime virkningstidspunkt = null;
        if (digitalPost.getVirkningsdato() != null) {
            virkningstidspunkt = new DateTime(digitalPost.getVirkningsdato().getTime());
        }

        boolean aapningskvittering = digitalPost.isAapningskvittering();
        SDPSikkerhetsnivaa sikkerhetsnivaa = digitalPost.getSikkerhetsnivaa().getXmlValue();
        SDPTittel tittel = new SDPTittel(digitalPost.getIkkeSensitivTittel(), forsendelse.getSpraakkode());
        SDPVarsler varsler = sdpVarsler(forsendelse);

        return new SDPDigitalPostInfo(virkningstidspunkt, null, aapningskvittering, sikkerhetsnivaa, tittel, varsler);
    }

    private SDPFysiskPostInfo sdpFysiskPostInfo(FysiskPost fysiskPost) {
    	if (fysiskPost == null) {
    		return null;
    	}

    	return new SDPFysiskPostInfo()
    		.withMottaker(sdpPostadresse(fysiskPost.getAdresse()))
    		.withPosttype(fysiskPost.getPosttype().sdpType)
    		.withUtskriftsfarge(fysiskPost.getUtskriftsfarge().sdpUtskriftsfarge)
    		.withRetur(new SDPFysiskPostRetur(fysiskPost.getReturhaandtering().sdpReturhaandtering, sdpPostadresse(fysiskPost.getReturadresse())));
    }

    private SDPFysiskPostadresse sdpPostadresse(KonvoluttAdresse adresse) {
    	SDPFysiskPostadresse sdpAdresse = new SDPFysiskPostadresse().withNavn(adresse.getNavn());
    	if (adresse.er(UTENLANDSK)) {
    		UpTo4ElementsOfList<String> adresselinjer = UpTo4ElementsOfList.extract(adresse.getAdresselinjer());
    		sdpAdresse.setUtenlandskAdresse(new SDPUtenlandskPostadresse(adresselinjer._1, adresselinjer._2, adresselinjer._3, adresselinjer._4, adresse.getLand(), adresse.getLandkode()));
    	} else if (adresse.er(NORSK)) {
    		UpTo4ElementsOfList<String> adresselinjer = UpTo4ElementsOfList.extract(adresse.getAdresselinjer());
    		sdpAdresse.setNorskAdresse(new SDPNorskPostadresse(adresselinjer._1, adresselinjer._2, adresselinjer._3, adresse.getPostnummer(), adresse.getPoststed()));
    	} else {
    		throw new IllegalArgumentException("Ukjent " + KonvoluttAdresse.class.getSimpleName() + "." + Type.class.getSimpleName() + ": " + adresse.getType());
    	}
    	return sdpAdresse;
    }



    private SDPVarsler sdpVarsler(final Forsendelse forsendelse) {
        String spraakkode = forsendelse.getSpraakkode();

        SDPEpostVarsel epostVarsel = sdpEpostVarsel(forsendelse.getDigitalPost().getEpostVarsel(), spraakkode);
        SDPSmsVarsel smsVarsel = sdpSmsVarsel(forsendelse.getDigitalPost().getSmsVarsel(), spraakkode);

        return new SDPVarsler(epostVarsel, smsVarsel);
    }

    private SDPSmsVarsel sdpSmsVarsel(final SmsVarsel smsVarsel, final String spraakkode) {
        if (smsVarsel != null) {
            SDPSmsVarselTekst smsVarselTekst = new SDPSmsVarselTekst(smsVarsel.getVarslingsTekst(), spraakkode);
            return new SDPSmsVarsel(smsVarsel.getMobilnummer(), smsVarselTekst, new SDPRepetisjoner(smsVarsel.getDagerEtter()));
        }
        return null;
    }

    private SDPEpostVarsel sdpEpostVarsel(final EpostVarsel epostVarsel, final String spraakkode) {
        if (epostVarsel != null) {
            SDPEpostVarselTekst epostVarselTekst = new SDPEpostVarselTekst(epostVarsel.getVarslingsTekst(), spraakkode);
            return new SDPEpostVarsel(epostVarsel.getEpostadresse(), epostVarselTekst, new SDPRepetisjoner(epostVarsel.getDagerEtter()));
        }
        return null;
    }

    static class UpTo4ElementsOfList<T> {
    	final T _1;
    	final T _2;
    	final T _3;
    	final T _4;

    	static <T> UpTo4ElementsOfList<T> extract(Iterable<T> iterable) {
    		return new UpTo4ElementsOfList<T>(iterable);
    	}

    	private UpTo4ElementsOfList(Iterable<T> iterable) {
    		Iterator<T> iterator = iterable.iterator();
    		_1 = iterator.hasNext() ? iterator.next() : null;
    		_2 = iterator.hasNext() ? iterator.next() : null;
    		_3 = iterator.hasNext() ? iterator.next() : null;
    		_4 = iterator.hasNext() ? iterator.next() : null;
    	}
    }

}
