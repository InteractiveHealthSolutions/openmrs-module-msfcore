package org.openmrs.module.msfcore.page.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.msfcore.api.MSFCoreService;
import org.openmrs.module.msfcore.audit.MSFCoreLog;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

public class AuditLogManagerPageController {

  public void controller(PageModel model, @SpringBean("msfCoreService") MSFCoreService msfCoreService,
      @RequestParam(value = "startTime", required = false) String startTime) {
    Date startDate = null;
    try {
      SimpleDateFormat dateISOFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      if(StringUtils.isNotBlank(startTime)) {
        startDate = dateISOFormatted.parse(startTime.replaceAll(",", ""));
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }
    List<MSFCoreLog> msfCoreLogs = msfCoreService.getMSFCoreLogs(startDate, null, null, null, null, null, null);
    model.addAttribute("msfCoreLogs", msfCoreLogs);
    model.addAttribute("startTime", startTime.replaceAll(",", ""));
  }
}
