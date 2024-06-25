package com.researchspace.dryad.rspaceadapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchspace.dryad.client.DryadClient;
import com.researchspace.dryad.model.DryadDataset;
import com.researchspace.dryad.model.DryadFile;
import com.researchspace.dryad.model.DryadSubmission;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.LicenseConfigInfo;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.Subject;
import com.researchspace.repository.spi.SubmissionMetadata;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DryadRSpaceRepositoryTest {

    @Mock
    private DryadClient dryadClient;
    @InjectMocks
    private DryadRSpaceRepository repoAdapter;
    @Mock
    private IDepositor author;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws MalformedURLException {
        MockitoAnnotations.openMocks(this);
        RepositoryConfig config = new RepositoryConfig(new URL("https://dryad-stg.cdlib.org/api/v2"), "token", "", "app.dryad");
        repoAdapter = new DryadRSpaceRepository();
        repoAdapter.configure(config);
        repoAdapter.setDryadClient(dryadClient);
    }

    @Test
    void testTestConnection() {
        when(dryadClient.testConnection()).thenReturn(true);
        assertTrue(repoAdapter.testConnection().isSucceeded());
    }

    @Test
    void testSubmitDeposit() throws IOException {
        when(dryadClient.createSubmission(any(DryadSubmission.class))).thenReturn(getDryadDataset());
        when(dryadClient.stageFile(any(String.class), any(String.class))).thenReturn(new DryadFile());
        SubmissionMetadata metadata = getTestSubmissionMetaData();
        File file = new File("src/test/resources/test.txt");
        RepositoryOperationResult dataset = repoAdapter.submitDeposit(null , file, metadata, null);
        assertTrue(dataset.isSucceeded());
        assertEquals("https://dryad-stg.cdlib.org/stash/edit/doi%3A10.7959%2Fdryad.5dv41ns2h/9pnE2U9VVe3mMQ", dataset.getUrl().toString());
    }

    @Test
    void testGetSubjects() {
        List<Subject> subjects = repoAdapter.getSubjects();
        assertFalse(subjects.isEmpty());
        assertEquals(48, subjects.size());
        assertEquals("Natural sciences", subjects.get(0).getName());
    }

    @Test
    void testGetLicenseConfigInfo() {
        LicenseConfigInfo licenseConfigInfo = repoAdapter.getLicenseConfigInfo();
        assertNotEquals(0, licenseConfigInfo.getLicenses().size());
        assertEquals("https://creativecommons.org/publicdomain/zero/1.0/", licenseConfigInfo.getLicenses().get(0).getLicenseDefinition().getUrl().toString());
    }

    private DryadDataset getDryadDataset() throws IOException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(IOUtils.resourceToString("/datasetCreationResponse.json", StandardCharsets.UTF_8), DryadDataset.class);
    }

    private SubmissionMetadata getTestSubmissionMetaData() throws IOException {
        SubmissionMetadata md = new SubmissionMetadata();
        when(author.getEmail()).thenReturn("email@somewhere.com");
        when(author.getUniqueName()).thenReturn("anyone");

        md.setAuthors(List.of(author));
        md.setContacts(List.of(author));
        md.setDescription("desc");
        md.setPublish(false);
        md.setSubjects(List.of("Other natural sciences"));
        md.setLicense(Optional.of(new URL("https://creativecommons.org/publicdomain/zero/1.0/")));
        md.setTitle("title");
        md.setOtherProperties(getTestOtherProperties());
        return md;
    }

    private Map<String, String> getTestOtherProperties() throws IOException {
        Map<String, String> otherProperties = new HashMap<>();
        otherProperties.put("funder", IOUtils.resourceToString("/otherProperties.json", StandardCharsets.UTF_8));
        return otherProperties;
    }

}
