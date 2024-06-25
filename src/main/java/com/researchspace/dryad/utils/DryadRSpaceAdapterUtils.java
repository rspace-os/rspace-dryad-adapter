package com.researchspace.dryad.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.researchspace.repository.spi.License;
import com.researchspace.repository.spi.LicenseDef;
import com.researchspace.repository.spi.Subject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DryadRSpaceAdapterUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns a list of subjects from dryad research domains json file.
     * @return list of subjects
     */
    public static List<Subject> getDryadSubjects() {
        List<Subject> result = new ArrayList<>();
        String json = null;
        try {
            //Read json file from resources folder
            json = IOUtils.resourceToString("/dryad-research-domains.json", Charset.defaultCharset());
            //Convert json string to JsonNode
            JsonNode jsonNode = mapper.readTree(json);
            //Iterate over jsonNode and create a list of subjects
            ArrayNode domains = jsonNode.withArray("domains");
            domains.forEach(node -> result.add(new Subject(node.asText())));

        } catch (IOException e) {
            log.error("Error reading dryad research domains json file", e);
        }

        return result;
    }

    public static List<License> getDryadLicenses() throws MalformedURLException {
        License license = new License();
        license.setLicenseDefinition(new LicenseDef(new URL("https://creativecommons.org/publicdomain/zero/1.0/"), "CC-0"));
        return Collections.singletonList(license);
    }
}
