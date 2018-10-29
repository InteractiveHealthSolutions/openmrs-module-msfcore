package org.openmrs.module.msfcore.fragment.controller.field;

import org.openmrs.api.context.Context;
import org.openmrs.module.msfcore.MSFCoreConfig;
import org.openmrs.module.msfcore.RegistrationAppUiUtils;
import org.openmrs.module.msfcore.api.MSFCoreService;
import org.openmrs.ui.framework.page.PageModel;

/**
 * This controller initializes all the MSF custom dropdowns, and also initializes the existing
 * patient fields for editing. Provenance is the first MSF custom registration field in the register
 * patient page, that's why this is done here.
 */
public class ProvenanceFragmentController {

    public void controller(PageModel model) {
        model.put("uiUtils", new RegistrationAppUiUtils());

        model.addAttribute("provenances", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_PROVENANCE_UUID));
        model.addAttribute("provenanceUuid", MSFCoreConfig.PERSON_ATTRIBUTE_PROVENANCE_UUID);

        model.addAttribute("nationalities", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_NATIONALITY_UUID));
        model.addAttribute("nationalityUuid", MSFCoreConfig.PERSON_ATTRIBUTE_NATIONALITY_UUID);

        model.addAttribute("conditionsOfLiving", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_CONDITION_OF_LIVING_UUID));
        model.addAttribute("conditionOfLivingUuid", MSFCoreConfig.PERSON_ATTRIBUTE_CONDITION_OF_LIVING_UUID);

        model.addAttribute("educations", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_EDUCATION_UUID));
        model.addAttribute("educationUuid", MSFCoreConfig.PERSON_ATTRIBUTE_EDUCATION_UUID);

        model.addAttribute("employmentStatuses", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_EMPLOYMENT_STATUS_UUID));
        model.addAttribute("employmentStatusUuid", MSFCoreConfig.PERSON_ATTRIBUTE_EMPLOYMENT_STATUS_UUID);

        model.addAttribute("maritalStatuses", Context.getService(MSFCoreService.class).getAllConceptAnswerNames(
                        MSFCoreConfig.CONCEPT_MARITAL_STATUS_UUID));
        model.addAttribute("maritalStatusUuid", MSFCoreConfig.PERSON_ATTRIBUTE_MARITAL_STATUS_UUID);

        model.addAttribute("oldFacilityCodeUuid", MSFCoreConfig.PERSON_ATTRIBUTE_OLD_FACILITY_CODE_UUID);
        model.addAttribute("otherIdNameUui", MSFCoreConfig.PERSON_ATTRIBUTE_OTHER_ID_NAME_UUID);
        model.put("phoneNumberUuid", MSFCoreConfig.PERSON_ATTRIBUTE_PHONE_NUMBER_UUID);
    }
}
