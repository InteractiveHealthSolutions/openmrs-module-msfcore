package org.openmrs.module.msfcore.page.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.msfcore.api.AuditService;
import org.openmrs.module.msfcore.audit.AuditLog;
import org.openmrs.module.msfcore.audit.AuditLog.Event;
import org.openmrs.ui.framework.page.PageModel;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class})
public class AuditLogManagerPageControllerTest {
  AuditLogManagerPageController controller;

  @Mock
  private AuditService auditService;

  @Mock
  private HttpServletRequest request;

  PageModel model;

  @Mock
  private UserService userService;

  AuditLog viewPatient;

  AuditLog registerPatient;

  AuditLog login;

  User user;

  Patient patient;

  @Before
  public void before() throws ParseException {
    controller = new AuditLogManagerPageController();
    model = new PageModel();
    Assert.assertNotNull(auditService);
    Assert.assertNotNull(request);
    Assert.assertNotNull(userService);
    UserContext userContext = Mockito.mock(UserContext.class);
    Context.setUserContext(userContext);
    Mockito.when(userContext.isAuthenticated()).thenReturn(true);
    PowerMockito.mockStatic(Context.class);
    Mockito.when(Context.getUserService()).thenReturn(userService);
    Mockito.when(Context.getUserService().getAllUsers()).thenReturn(new ArrayList<User>());
    user = new User(1);
    user.setUsername("hacker");
    patient = Mockito.mock(Patient.class);
    viewPatient = new AuditLog(Event.VIEW_PATIENT, "viewed patient dashboard", user);
    viewPatient.setPatient(patient);
    viewPatient.setDate(new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-01"));
    registerPatient = new AuditLog(Event.REGISTER_PATIENT, "registered patient", user);
    registerPatient.setPatient(new Patient(2));
    registerPatient.setDate(new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-25"));
    login = new AuditLog(Event.LOGIN, "user logged in", user);
    login.setUser(user);
    login.setDate(new SimpleDateFormat("yyyy-MM-dd").parse("2018-08-01"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get_defaultWithoutLogs() {
    controller.controller(model, auditService, "", "", request, "", null, "");
    Assert.assertEquals(0, ((List<AuditLog>) model.getAttribute("auditLogs")).size());
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    Assert.assertNull(model.getAttribute("selectedEvents"));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get_defaultWithoutLogsButWithUserSuggestions() {
    // user suggestions should include both unique username and system id
    User user2 = new User(2);
    user2.setSystemId("3-4");
    user2.setUsername("hacker2");
    User user3 = new User(2);
    user3.setSystemId("3-4");
    user3.setUsername("hacker3");
    Mockito.when(Context.getUserService().getAllUsers()).thenReturn(Arrays.asList(user, user2, user3));
    controller.controller(model, auditService, "", "", request, "", null, "");
    Assert.assertEquals(0, ((List<AuditLog>) model.getAttribute("auditLogs")).size());
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    Assert.assertNull(model.getAttribute("selectedEvents"));
    List<String> userSuggestions = (List<String>) model.getAttribute("userSuggestions");
    Assert.assertEquals(4, userSuggestions.size());
    Assert.assertThat(userSuggestions, CoreMatchers.hasItems("hacker", "hacker2", "3-4", "hacker3"));
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get_defaultWithLogs() {
    Mockito.when(auditService.getAuditLogs(null, null, null, null, null, null, null, null))
        .thenReturn(Arrays.asList(viewPatient, registerPatient, login));
    controller.controller(model, auditService, "", "", request, "", null, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(3));
    Assert.assertThat(logs.get(0), CoreMatchers.is(viewPatient));
    Assert.assertThat(logs.get(1), CoreMatchers.is(registerPatient));
    Assert.assertThat(logs.get(2), CoreMatchers.is(login));
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    Assert.assertNull(model.getAttribute("selectedEvents"));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_inRangeWithLogs() throws ParseException {
    Mockito
        .when(auditService.getAuditLogs(new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-22"),
            new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-29"), null, null, null, null, null, null))
        .thenReturn(Arrays.asList(registerPatient));
    controller.controller(model, auditService, "2018-07-22 00:00:00", "2018-07-29 00:00:00", request, "", null, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(1));
    Assert.assertThat(logs.get(0), CoreMatchers.is(registerPatient));
    Assert.assertEquals("2018-07-22 00:00:00", (String) model.getAttribute("startTime"));
    Assert.assertEquals("2018-07-29 00:00:00", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    Assert.assertNull(model.getAttribute("selectedEvents"));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_withSelectedEvents() throws ParseException {
    Mockito
        .when(
            auditService.getAuditLogs(null, null, Arrays.asList(Event.VIEW_PATIENT, Event.REGISTER_PATIENT), null, null, null, null, null))
        .thenReturn(Arrays.asList(viewPatient, registerPatient));
    Mockito.when(request.getParameterValues("events")).thenReturn(new String[]{Event.VIEW_PATIENT.name(), Event.REGISTER_PATIENT.name()});
    controller.controller(model, auditService, "", "", request, "", null, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(2));
    Assert.assertThat(logs.get(0), CoreMatchers.is(viewPatient));
    Assert.assertThat(logs.get(1), CoreMatchers.is(registerPatient));
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    String[] events = (String[]) model.getAttribute("selectedEvents");
    Assert.assertThat(events[0], CoreMatchers.is(Event.VIEW_PATIENT.name()));
    Assert.assertThat(events[1], CoreMatchers.is(Event.REGISTER_PATIENT.name()));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_withCreatedBy() {
    Mockito.when(Context.getUserService().getUserByUsername("hacker")).thenReturn(user);
    Mockito.when(auditService.getAuditLogs(null, null, null, user, null, null, null, null))
        .thenReturn(Arrays.asList(viewPatient, registerPatient, login));
    controller.controller(model, auditService, "", "", request, "hacker", null, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(3));
    Assert.assertThat(logs.get(0), CoreMatchers.is(viewPatient));
    Assert.assertThat(logs.get(1), CoreMatchers.is(registerPatient));
    Assert.assertThat(logs.get(2), CoreMatchers.is(login));
    Assert.assertThat(logs.get(0).getCreator(), CoreMatchers.is(user));
    Assert.assertThat(logs.get(1).getCreator(), CoreMatchers.is(user));
    Assert.assertThat(logs.get(2).getCreator(), CoreMatchers.is(user));
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    Assert.assertNull(model.getAttribute("selectedEvents"));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("hacker", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_inRange_selectedEvents_Created_by() throws ParseException {
    Mockito.when(Context.getUserService().getUserByUsername("hacker")).thenReturn(user);
    Mockito
        .when(auditService.getAuditLogs(new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-22"),
            new SimpleDateFormat("yyyy-MM-dd").parse("2018-07-29"), Arrays.asList(Event.REGISTER_PATIENT), user, null, null, null, null))
        .thenReturn(Arrays.asList(registerPatient));
    Mockito.when(request.getParameterValues("events")).thenReturn(new String[]{Event.REGISTER_PATIENT.name()});
    controller.controller(model, auditService, "2018-07-22 00:00:00", "2018-07-29 00:00:00", request, "hacker", null, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(1));
    Assert.assertThat(logs.get(0), CoreMatchers.is(registerPatient));
    Assert.assertThat(logs.get(0).getCreator(), CoreMatchers.is(user));
    Assert.assertThat(logs.get(0).getEvent(), CoreMatchers.is(Event.REGISTER_PATIENT));
    Assert.assertEquals("2018-07-22 00:00:00", (String) model.getAttribute("startTime"));
    Assert.assertEquals("2018-07-29 00:00:00", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    String[] events = (String[]) model.getAttribute("selectedEvents");
    Assert.assertThat(events.length, CoreMatchers.is(1));
    Assert.assertThat(events[0], CoreMatchers.is(Event.REGISTER_PATIENT.name()));
    Assert.assertEquals(0, ((List<User>) model.getAttribute("userSuggestions")).size());
    Assert.assertEquals("hacker", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_quick_patientViewedByUser() {
    Mockito.when(Context.getUserService().getUserByUsername("hacker")).thenReturn(user);
    Mockito.when(auditService.getAuditLogs(null, null, Arrays.asList(Event.VIEW_PATIENT), null, null, Arrays.asList(user), null, null))
        .thenReturn(Arrays.asList(viewPatient));
    controller.controller(model, auditService, "", "", request, "", null, "hacker");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(1));
    Assert.assertThat(logs.get(0), CoreMatchers.is(viewPatient));
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    String[] events = (String[]) model.getAttribute("selectedEvents");
    Assert.assertThat(events.length, CoreMatchers.is(1));
    Assert.assertThat(events[0], CoreMatchers.is(Event.VIEW_PATIENT.name()));
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("hacker", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("", (String) model.getAttribute("patientDisplay"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void post_filter_quick_usersViewedAPatient() {
    PatientIdentifier id = new PatientIdentifier();
    id.setIdentifier("MSF_ID_1");
    Mockito.when(patient.getPersonName()).thenReturn(new PersonName("John", "D", "Patient"));
    Mockito.when(patient.getPatientIdentifier()).thenReturn(id);
    Mockito.when(auditService.getAuditLogs(null, null, Arrays.asList(Event.VIEW_PATIENT), null, Arrays.asList(patient), null, null, null))
        .thenReturn(Arrays.asList(viewPatient));
    controller.controller(model, auditService, "", "", request, "", patient, "");
    List<AuditLog> logs = (List<AuditLog>) model.getAttribute("auditLogs");
    Assert.assertThat(logs.size(), CoreMatchers.is(1));
    Assert.assertThat(logs.get(0), CoreMatchers.is(viewPatient));
    Assert.assertEquals("", (String) model.getAttribute("startTime"));
    Assert.assertEquals("", (String) model.getAttribute("endTime"));
    Assert.assertEquals(Arrays.asList(Event.values()), (List<Event>) model.getAttribute("events"));
    String[] events = (String[]) model.getAttribute("selectedEvents");
    Assert.assertThat(events.length, CoreMatchers.is(1));
    Assert.assertThat(events[0], CoreMatchers.is(Event.VIEW_PATIENT.name()));
    Assert.assertEquals("", (String) model.getAttribute("selectedUser"));
    Assert.assertEquals("", (String) model.getAttribute("selectedViewer"));
    Assert.assertEquals("John D Patient #MSF_ID_1", (String) model.getAttribute("patientDisplay"));
  }
}
