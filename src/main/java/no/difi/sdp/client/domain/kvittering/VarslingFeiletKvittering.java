package no.difi.sdp.client.domain.kvittering;

import java.util.Date;

public class VarslingFeiletKvittering extends ForretningsKvittering {

    private Varslingskanal varslingskanal;
    private String beskrivelse;

    private VarslingFeiletKvittering(Date tidspunkt, String konversasjonsId, String refToMessageId, Varslingskanal varslingskanal) {
        super(tidspunkt, konversasjonsId, refToMessageId);
        this.varslingskanal = varslingskanal;
    }

    public Varslingskanal getVarslingskanal() {
        return varslingskanal;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public static Builder builder(Date tidspunkt, String konversasjonsId, String refToMessageId, Varslingskanal varslingskanal) {
        return new Builder(tidspunkt, konversasjonsId, refToMessageId, varslingskanal);
    }

    public static class Builder {
        private VarslingFeiletKvittering target;
        private boolean built = false;

        public Builder(Date tidspunkt, String konversasjonsId, String refToMessageId, Varslingskanal varslingskanal) {
            target = new VarslingFeiletKvittering(tidspunkt, konversasjonsId, refToMessageId, varslingskanal);
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
}