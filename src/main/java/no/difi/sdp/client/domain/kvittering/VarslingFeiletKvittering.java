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

public class VarslingFeiletKvittering extends ForretningsKvittering {

    private Varslingskanal varslingskanal;
    private String beskrivelse;

    private VarslingFeiletKvittering(EbmsApplikasjonsKvittering applikasjonsKvittering, Varslingskanal varslingskanal) {
        super(applikasjonsKvittering);
        this.varslingskanal = varslingskanal;
    }

    public Varslingskanal getVarslingskanal() {
        return varslingskanal;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "konversasjonsId=" + getKonversasjonsId() +
                ", varslingskanal=" + varslingskanal +
                ", beskrivelse='" + beskrivelse + '\'' +
                '}';
    }

    public static Builder builder(EbmsApplikasjonsKvittering applikasjonsKvittering, Varslingskanal varslingskanal) {
        return new Builder(applikasjonsKvittering, varslingskanal);
    }

    public static class Builder {
        private VarslingFeiletKvittering target;
        private boolean built = false;

        public Builder(EbmsApplikasjonsKvittering applikasjonsKvittering, Varslingskanal varslingskanal) {
            target = new VarslingFeiletKvittering(applikasjonsKvittering, varslingskanal);
        }

        public Builder beskrivelse(String beskrivelse) {
            target.beskrivelse = beskrivelse;
            return this;
        }

        public VarslingFeiletKvittering build() {
            if (built) throw new IllegalStateException("Can't build twice");
            built = true;
            return target;
        }
    }

    public enum Varslingskanal {
        SMS,
        EPOST
    }
}
