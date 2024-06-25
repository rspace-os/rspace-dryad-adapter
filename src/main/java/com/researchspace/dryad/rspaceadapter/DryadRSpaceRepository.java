package com.researchspace.dryad.rspaceadapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchspace.dryad.client.DryadClient;
import com.researchspace.dryad.client.DryadClientImpl;
import com.researchspace.dryad.model.DryadAuthor;
import com.researchspace.dryad.model.DryadDataset;
import com.researchspace.dryad.model.DryadFunder;
import com.researchspace.dryad.model.DryadSubmission;
import com.researchspace.dryad.utils.DryadRSpaceAdapterUtils;
import com.researchspace.repository.spi.IDepositor;
import com.researchspace.repository.spi.IRepository;
import com.researchspace.repository.spi.LicenseConfigInfo;
import com.researchspace.repository.spi.RepositoryConfig;
import com.researchspace.repository.spi.RepositoryConfigurer;
import com.researchspace.repository.spi.RepositoryOperationResult;
import com.researchspace.repository.spi.Subject;
import com.researchspace.repository.spi.SubmissionMetadata;
import com.researchspace.repository.spi.properties.RepoProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class DryadRSpaceRepository implements IRepository, RepositoryConfigurer {

    private DryadClient dryadClient;
    private String dryadBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(RepositoryConfig repositoryConfig) {
        this.dryadClient = new DryadClientImpl(repositoryConfig.getServerURL(), repositoryConfig.getIdentifier());
        URL apiUrl = repositoryConfig.getServerURL();
        this.dryadBaseUrl = apiUrl.getProtocol() + "://" + apiUrl.getHost();
    }

    @Override
    public RepositoryOperationResult submitDeposit(IDepositor iDepositor, File file, SubmissionMetadata submissionMetadata, RepositoryConfig repositoryConfig) {
        // Create submission from metadata.
        DryadSubmission submission = createDryadSubmission(submissionMetadata);
        // Create an in-progress dataset from submission.
        try {
            DryadDataset inProgressDataset = dryadClient.createSubmission(submission);
            // Upload file to in-progress dataset.
            dryadClient.stageFile(inProgressDataset.getIdentifier(), file.getName(), file);
            String editUrl = dryadBaseUrl + inProgressDataset.getEditLink();
            log.debug("editUrl: {}", editUrl);
            URL editURL = new URL(editUrl);

            return new RepositoryOperationResult(true, "Export uploaded to dryad successfully.", editURL);

        } catch (RestClientException e) {
            log.error("RestClientException occurred while submitting to dryad", e);
            return new RepositoryOperationResult(false, "RestClientException occurred while submitting to dryad", null);
        } catch (MalformedURLException e) {
            log.error("MalformedURLException occurred while submitting to dryad", e);
            return new RepositoryOperationResult(false, "MalformedURLException occurred while submitting to dryad", null);
        } catch (IOException e) {
            log.error("IOException occurred while submitting to dryad", e);
            return new RepositoryOperationResult(false, "IOException occurred while submitting to dryad", null);
        }
    }

    private DryadSubmission createDryadSubmission(SubmissionMetadata submissionMetadata) {
        DryadSubmission dryadSubmission = new DryadSubmission();
        dryadSubmission.setTitle(submissionMetadata.getTitle());
        dryadSubmission.setDryadAbstract(submissionMetadata.getDescription());
        dryadSubmission.setFieldOfScience(submissionMetadata.getSubjects().get(0));
        dryadSubmission.setAuthors(getDryadAuthors(submissionMetadata.getAuthors()));
        dryadSubmission.setFunders(getDryadFunders(submissionMetadata.getOtherProperties()));
        dryadSubmission.setLicense(String.valueOf(submissionMetadata.getLicense()));
        return dryadSubmission;
    }

    private List<DryadFunder> getDryadFunders(Map<String, String> otherProperties) {
        List<DryadFunder> dryadFunders;
        try {
            dryadFunders = new ArrayList<>(List.of(objectMapper.readValue(otherProperties.get("funder"), DryadFunder.class)));
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException occurred while getting funders", e);
            return new ArrayList<>();
        }

        return dryadFunders;
    }

    private List<DryadAuthor> getDryadAuthors(List<IDepositor> authors) {
        List<DryadAuthor> dryadAuthors = new ArrayList<>();
        for (IDepositor author : authors) {
            DryadAuthor dryadAuthor = new DryadAuthor();
            dryadAuthor.setFirstName(author.getUniqueName());
            dryadAuthor.setEmail(author.getEmail());
            dryadAuthors.add(dryadAuthor);
        }
        return dryadAuthors;
    }

    @Override
    public RepositoryOperationResult testConnection() {
        try {
            if (dryadClient.testConnection()) {
                return new RepositoryOperationResult(true, "Test connection OK!", null);
            } else {
                return new RepositoryOperationResult(false, "Test connection failed - please check settings.", null);
            }
        } catch (RestClientException e) {
            log.error("Couldn't perform test action {}" + e.getMessage());
            return new RepositoryOperationResult(false, "Test connection failed - " + e.getMessage(), null);
        }
    }

    @Override
    public RepositoryConfigurer getConfigurer() {
        return this;
    }

    @Override
    public List<Subject> getSubjects() {
        return DryadRSpaceAdapterUtils.getDryadSubjects();
    }

    @SneakyThrows
    @Override
    public LicenseConfigInfo getLicenseConfigInfo() {
        return new LicenseConfigInfo(true, false, DryadRSpaceAdapterUtils.getDryadLicenses());
    }

    @Override
    public Map<String, RepoProperty> getOtherProperties() {
        return new HashMap<>();
    }
}

