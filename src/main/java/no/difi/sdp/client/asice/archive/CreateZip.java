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
package no.difi.sdp.client.asice.archive;

import no.difi.sdp.client.asice.AsicEAttachable;
import no.difi.sdp.client.domain.exceptions.RuntimeIOException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CreateZip {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public Archive zipIt(List<AsicEAttachable> files) {
        ByteArrayOutputStream archive = null;
        ZipArchiveOutputStream zipOutputStream = null;
        try {
            archive = new ByteArrayOutputStream();
            zipOutputStream = new ZipArchiveOutputStream(archive);
            zipOutputStream.setEncoding(Charsets.UTF_8.name());
            zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
            for (AsicEAttachable file : files) {
                log.trace("Adding " + file.getFileName() + " to archive. Size in bytes before compression: " + file.getBytes().length);
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(file.getFileName());
                zipEntry.setSize(file.getBytes().length);

                zipOutputStream.putArchiveEntry(zipEntry);
                IOUtils.copy(new ByteArrayInputStream(file.getBytes()), zipOutputStream);
                zipOutputStream.closeArchiveEntry();
            }
            zipOutputStream.finish();
            zipOutputStream.close();

            return new Archive(archive.toByteArray());
        }
        catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        finally {
            IOUtils.closeQuietly(archive);
            IOUtils.closeQuietly(zipOutputStream);
        }
    }
}
