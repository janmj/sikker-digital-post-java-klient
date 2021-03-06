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
package no.difi.sdp.client.domain;

public class TekniskAvsender {

    public final String organisasjonsnummer;
    public final Noekkelpar noekkelpar;

    private TekniskAvsender(String organisasjonsnummer, Noekkelpar noekkelpar) {
        this.organisasjonsnummer = organisasjonsnummer;
        this.noekkelpar = noekkelpar;
    }

    /**
     * @param organisasjonsnummer Organisasjonsnummeret til avsender av brevet.
     * @param noekkelpar Avsenders nøkkelpar: signert virksomhetssertifikat og tilhørende privatnøkkel.
     */
    public static Builder builder(String organisasjonsnummer, Noekkelpar noekkelpar) {
        return new Builder(organisasjonsnummer, noekkelpar);
    }

    public static class Builder {

        private final TekniskAvsender target;
        private boolean built = false;

        private Builder(String orgNummer, Noekkelpar noekkelpar) {
            target = new TekniskAvsender(orgNummer, noekkelpar);
        }

        public TekniskAvsender build() {
            if (built) throw new IllegalStateException("Can't build twice");
            built = true;
            return this.target;
        }
    }
}
