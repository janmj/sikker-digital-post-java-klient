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
package no.difi.sdp.client.domain.fysisk_post;

import no.difi.begrep.sdp.schema_v10.SDPFysiskPostType;

public enum Posttype {
	A_PRIORITERT (SDPFysiskPostType.A),
	B_OEKONOMI   (SDPFysiskPostType.B);

	public final SDPFysiskPostType sdpType;

	Posttype(SDPFysiskPostType sdpType) {
		this.sdpType = sdpType;
	}
}
