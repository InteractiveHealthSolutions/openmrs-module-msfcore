/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * 
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.msfcore.api.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.msfcore.MSFCoreConfig;
import org.openmrs.module.msfcore.api.DHISService;
import org.openmrs.module.msfcore.api.dao.MSFCoreDao;
import org.openmrs.module.msfcore.dhis2.Data;
import org.openmrs.module.msfcore.dhis2.Parameters;
import org.openmrs.module.msfcore.dhis2.PatientTrackableAttributes;
import org.openmrs.module.msfcore.dhis2.TrackerInstance;
import org.openmrs.util.OpenmrsUtil;

public class DHISServiceImpl extends BaseOpenmrsService implements DHISService {

    MSFCoreDao dao;

    /**
     * Injected in moduleApplicationContext.xml
     */
    public void setDao(MSFCoreDao dao) {
        this.dao = dao;
    }

    private Log log = LogFactory.getLog(getClass());

    public LocationAttribute getLocationUidAttribute(Location location) {
        return getLocationAttribute(location, MSFCoreConfig.LOCATION_ATTR_TYPE_UID_UUID);
    }

    private LocationAttribute getLocationAttribute(Location location, String attributeTypeUuid) {
        if (location != null) {
            List<LocationAttribute> attrributes = dao.getLocationAttributeByTypeAndLocation(Context.getLocationService()
                            .getLocationAttributeTypeByUuid(attributeTypeUuid), location);
            if (!attrributes.isEmpty()) {
                return attrributes.get(0);
            }
        }
        return null;
    }
    public String getLocationDHISUid(Location location) {
        if (location == null) {
            return null;
        }
        return getLocationAttribute(getLocationAttribute(location, MSFCoreConfig.LOCATION_ATTR_TYPE_UID_UUID));
    }

    private String getLocationAttribute(LocationAttribute locAttribute) {
        if (locAttribute != null) {
            return locAttribute.getValue().toString();
        }
        return "";
    }
    private File getDHIS2MappingsFile() {
        File msfcoreStorage = new File(OpenmrsUtil.getApplicationDataDirectory() + File.separator + "msfcore");
        if (!msfcoreStorage.exists()) {
            msfcoreStorage.mkdirs();
        }
        return new File(msfcoreStorage.getAbsoluteFile() + File.separator + "dhis-mappings.properties");
    }

    public void transferDHISMappingsToDataDirectory() {
        File mappingDestination = getDHIS2MappingsFile();
        if (mappingDestination.exists()) {
            return;
        }
        File mappingsFile = new File(getClass().getClassLoader().getResource("dhis-mappings.properties").getFile());
        try {
            FileUtils.copyFile(mappingsFile, mappingDestination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getDHISMappings() {
        Properties prop = new Properties();
        File mappingFile = getDHIS2MappingsFile();
        if (!mappingFile.exists()) {
            mappingFile = new File(getClass().getClassLoader().getResource("dhis-mappings.properties").getFile());
        }
        try {
            prop.load(new FileInputStream(mappingFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return prop;
    }

    String getOpenHIMClientUsername() {
        String username = Context.getRuntimeProperties().getProperty(MSFCoreConfig.PROP_OPENHIM_USERNAME);
        return StringUtils.isBlank(username) ? "msf" : username;
    }

    String getOpenHIMClientPassword() {
        String pass = Context.getRuntimeProperties().getProperty(MSFCoreConfig.PROP_OPENHIM_PASSWORD);
        return StringUtils.isBlank(pass) ? "msf" : pass;
    }

    String getDHIS2Username() {
        String username = Context.getRuntimeProperties().getProperty(MSFCoreConfig.PROP_DHIS2_USERNAME);
        return StringUtils.isBlank(username) ? "admin" : username;
    }

    String getDHIS2Password() {
        String pass = Context.getRuntimeProperties().getProperty(MSFCoreConfig.PROP_DHIS2_PASSWORD);
        return StringUtils.isBlank(pass) ? "district" : pass;
    }

    private List<PersonAttribute> getPersonAttributeByTypeAndPerson(PersonAttributeType personAttributeType, Person person) {
        return dao.getPersonAttributeByTypeAndPerson(personAttributeType, person);
    }

    private String getPersonAttributeValue(Person person, String personAttributeTypeUuid) {
        List<PersonAttribute> attrs = getPersonAttributeByTypeAndPerson(Context.getPersonService().getPersonAttributeTypeByUuid(
                        personAttributeTypeUuid), person);
        if (!attrs.isEmpty()) {
            return attrs.get(0).getValue();
        }
        return null;
    }

    private List<PatientIdentifier> getPatientIdentifierByTypeAndPatient(PatientIdentifierType type, Patient patient) {
        return dao.getPatientIdentifierByTypeAndPatient(type, patient);
    }

    private String getPatientIdentifierValue(Patient patient, String patientIndentifierTypeUuid) {
        List<PatientIdentifier> ids = getPatientIdentifierByTypeAndPatient(Context.getPatientService().getPatientIdentifierTypeByUuid(
                        patientIndentifierTypeUuid), patient);
        if (!ids.isEmpty()) {
            return ids.get(0).getIdentifier();
        }
        return null;
    }
    private void setIdentifiers(Patient patient, Map<String, String> attributes) {
        String patientId = getPatientIdentifierValue(patient, MSFCoreConfig.PATIENT_ID_TYPE_PRIMARY_UUID);
        if (StringUtils.isNotBlank(patientId)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.PATIENT_NUMBER.name()), patientId);
        }
        String UNHCR = getPatientIdentifierValue(patient, MSFCoreConfig.PATIENT_ID_TYPE_UNHCR_ID_UUID);
        if (StringUtils.isNotBlank(UNHCR)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.REFUGEE_UNHCR.name()), "true");
        }
        String UNRWA = getPatientIdentifierValue(patient, MSFCoreConfig.PATIENT_ID_TYPE_UNRWA_ID_UUID);
        if (StringUtils.isNotBlank(UNRWA)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.REFUGEE_UNRWA.name()), "true");
        }
        String oldPatientId = getPatientIdentifierValue(patient, MSFCoreConfig.PATIENT_ID_TYPE_OLD_PATIENT_ID_UUID);
        if (StringUtils.isNotBlank(oldPatientId)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.OLD_PATIENT_ID.name()), oldPatientId);
        }
    }

    private void setPersonAttributes(Patient patient, Map<String, String> attributes) {
        String provenance = getPersonAttributeValue(patient, MSFCoreConfig.PERSON_ATTRIBUTE_PROVENANCE_UUID);
        if (provenance != null) {
            // TODO fix Value 'Buffer Zone resident' is not a valid option for
            // attribute NdCX1An92BP and option set zZhDsRgO0tJ
            /*
             * attributes.put(getDHISMappings().getProperty(
             * PatientTrackableAttributes.PROVENANCE.name()),
             * Context.getConceptService().getConcept(Integer.parseInt(
             * provenance)).getName().getName());
             */
        }
        String nationality = getPersonAttributeValue(patient, MSFCoreConfig.PERSON_ATTRIBUTE_NATIONALITY_UUID);
        if (StringUtils.isNotBlank(nationality)) {
            // TODO fix "Value 'Belarus' is not a valid option for attribute
            // hLYUwDODGZf and option set OPrxrevD0xp
            /*
             * attributes.put(getDHISMappings().getProperty(
             * PatientTrackableAttributes.NATIONALITY.name()),
             * Context.getConceptService().getConcept(Integer.parseInt(
             * nationality)).getName().getName());
             */
        }
        String conditionOfLiving = getPersonAttributeValue(patient, MSFCoreConfig.PERSON_ATTRIBUTE_CONDITION_OF_LIVING_UUID);
        if (StringUtils.isNotBlank(conditionOfLiving)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.CONDITION_OF_LIVING.name()), Context
                            .getConceptService().getConcept(Integer.parseInt(conditionOfLiving)).getName().getName());
        }
        String maritalStatus = getPersonAttributeValue(patient, MSFCoreConfig.PERSON_ATTRIBUTE_MARITAL_STATUS_UUID);
        if (StringUtils.isNotBlank(maritalStatus)) {
            // TODO fix Value 'Single' is not a valid option for attribute
            // Ym6qbECXEZt and option set v7xyRqfrzBQ
            /*
             * attributes.put(getDHISMappings().getProperty(
             * PatientTrackableAttributes.MARITAL_STATUS.name()),
             * Context.getConceptService().getConcept(Integer.parseInt(
             * maritalStatus)).getName().getName());
             */
        }
        String phoneNumber = getPersonAttributeValue(patient, MSFCoreConfig.PERSON_ATTRIBUTE_PHONENUMBER_UUID);
        if (StringUtils.isNotBlank(phoneNumber)) {
            attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.PHONE_NUMBER.name()), (String) phoneNumber);
        }
        // TODO add MOTHER_NAME, EDUCATION(???), PATIENT_TYPE?

    }

    private TrackerInstance generateTrackerInstanceForPatient(Patient patient) {
        Location defaultLocation = Context.getLocationService().getDefaultLocation();
        TrackerInstance trackerInstance = new TrackerInstance();
        trackerInstance.setUrl(TrackerInstance.generateUrl(getDHIS2Username(), getDHIS2Password(), Context.getAdministrationService()
                        .getGlobalProperty(MSFCoreConfig.GP_DHIS_HOST)));
        trackerInstance.setProgram(Context.getAdministrationService().getGlobalProperty(MSFCoreConfig.GP_DHIS_NCD_PROGRAM_UID));
        trackerInstance.setProgramStage(Context.getAdministrationService().getGlobalProperty(MSFCoreConfig.GP_DHIS_NCD_PROGRAMSTAGE_UID));
        trackerInstance.setTrackedEntity(Context.getAdministrationService().getGlobalProperty(MSFCoreConfig.GP_DHIS_TRACKENTITYTYPE_UID));
        Data data = new Data();
        // TODO do we need set dataElements
        Parameters parameters = new Parameters();
        // TODO introduce programs for MSF and control when to enroll patient
        // etc then call this functionality there
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        parameters.setEventDate(today);
        parameters.setProgramDate(today);
        if (defaultLocation != null) {
            parameters.setOrgUnit(getLocationDHISUid(defaultLocation));
        }
        data.setParameters(parameters);

        // add patient details
        Map<String, String> attributes = data.getAttributes();
        attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.FIRST_NAME.name()), patient.getGivenName());
        attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.LAST_NAME.name()), patient.getFamilyName());
        attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.AGE_IN_YEARS.name()), String.valueOf(patient.getAge()));
        attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.DATE_OF_BIRTH.name()), sdf.format(patient.getBirthdate()));
        // TODO fix Value 'F' is not a valid option for attribute BIrTkunTimU
        // and option set R1iJ6BKkufb
        // attributes.put(getDHISMappings().getProperty(PatientTrackableAttributes.SEX.name()),
        // patient.getGender());
        setIdentifiers(patient, attributes);
        setPersonAttributes(patient, attributes);
        data.setAttributes(attributes);

        trackerInstance.setData(data);
        return trackerInstance;
    }

    public String postTrackerInstanceThroughOpenHimForAPatient(Patient patient) {
        TrackerInstance trackerInstance = generateTrackerInstanceForPatient(patient);
        DefaultHttpClient client = null;
        String payload = "";
        String url = Context.getAdministrationService().getGlobalProperty(MSFCoreConfig.GP_OPENHIM_TRACKER_URL);
        try {
            URL dhisURL = new URL(url);
            HttpHost targetHost = new HttpHost(dhisURL.getHost(), dhisURL.getPort(), dhisURL.getProtocol());
            client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(dhisURL.getPath());
            BasicHttpContext localcontext = new BasicHttpContext();
            setHeaders(httpPost, localcontext);
            httpPost.setEntity(new StringEntity(trackerInstance.toString()));
            HttpResponse response = client.execute(targetHost, httpPost, localcontext);
            HttpEntity entity = response.getEntity();
            if (entity != null && response.getStatusLine().getStatusCode() == 200) {
                payload = EntityUtils.toString(entity);
                log.info("POST: to " + url + " returned " + payload);
            }
        } catch (Exception ex) {
            log.error("POST: to " + url + " returned " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        return payload;
    }

    private void setHeaders(HttpPost httpPost, HttpContext localcontext) throws AuthenticationException {
        Credentials creds = new UsernamePasswordCredentials(getOpenHIMClientUsername(), getOpenHIMClientPassword());
        Header bs = new BasicScheme().authenticate(creds, httpPost, localcontext);
        httpPost.addHeader("Authorization", bs.getValue());
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Accept", "application/json");
    }

}
